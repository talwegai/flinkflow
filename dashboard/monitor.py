# Copyright 2026 Talweg Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from nicegui import ui
import os
import random
from datetime import datetime
import time
import requests

from k8s_client import (
    K8S_AVAILABLE, discover_flink_pods, discover_flowlets,
    start_log_stream, consume_log_queue
)
from flink_client import (
    FLINK_BASE_URL, fetch_flink_jobs, get_job_details, get_job_metrics, get_job_plan
)
from ui_components import (
    generate_mermaid_from_plan, _build_pod_row, _build_flowlet_row
)

# ─── Metrics State ────────────────────────────────────────────────────────────

stats = {"throughput": 0, "latency": 0, "uptime": "00:00:00"}
start_time = datetime.now()

# ─── UI Layout ────────────────────────────────────────────────────────────────

ui.query("body").style(
    "background-color: #0f172a; color: #f8fafc; font-family: 'Inter', sans-serif;"
)

# Header
with ui.header().classes("items-center bg-slate-900 border-b border-slate-800 p-4 shadow-2xl"):
    ui.icon("analytics", size="32px").classes("text-blue-400")
    with ui.column().classes("gap-0"):
        ui.label("Flinkflow").classes("text-2xl font-black tracking-tighter leading-none")
        ui.label("MONITORING ENGINE").classes("text-[10px] text-slate-500 font-bold tracking-[0.2em]")
    ui.space()
    with ui.row().classes("items-center gap-4"):
        ui.badge(
            "K8S: LIVE" if K8S_AVAILABLE else "K8S: DEMO",
            color="green" if K8S_AVAILABLE else "amber",
        ).classes("text-[10px] px-2 py-1")
        
        # Flink API Badge
        flink_api_reachable = False
        try:
            flink_api_reachable = requests.get(f"{FLINK_BASE_URL}/overview", timeout=0.5).status_code == 200
        except:
            pass
        
        ui.badge(
            "FLINK: LIVE" if flink_api_reachable else "FLINK: OFFLINE",
            color="green" if flink_api_reachable else "red",
        ).classes("text-[10px] px-2 py-1")

        ui.badge("LIVE", color="green").classes(
            "px-3 py-1 ring-2 ring-green-900 ring-offset-2 ring-offset-slate-900 animate-pulse"
        )

# Main container
with ui.column().classes("w-full p-8 max-w-7xl mx-auto gap-6"):

    # ── Section 1 · Metrics Overview ─────────────────────────────────────────
    with ui.row().classes("w-full gap-4"):
        with ui.card().classes(
            "flex-1 bg-slate-800/50 backdrop-blur-md border-slate-700 p-6 shadow-xl "
            "hover:border-blue-500/50 transition-all"
        ):
            ui.label("THROUGHPUT").classes("text-[10px] text-slate-400 font-bold tracking-widest")
            throughput_label = ui.label("0 rec/s").classes("text-4xl font-mono text-blue-400 mt-2 font-black")

        with ui.card().classes(
            "flex-1 bg-slate-800/50 backdrop-blur-md border-slate-700 p-6 shadow-xl "
            "hover:border-purple-500/50 transition-all"
        ):
            ui.label("LATENCY").classes("text-[10px] text-slate-400 font-bold tracking-widest")
            latency_label = ui.label("0 ms").classes("text-4xl font-mono text-purple-400 mt-2 font-black")

        with ui.card().classes(
            "flex-1 bg-slate-800/50 backdrop-blur-md border-slate-700 p-6 shadow-xl "
            "hover:border-green-500/50 transition-all"
        ):
            ui.label("UPTIME").classes("text-[10px] text-slate-400 font-bold tracking-widest")
            uptime_label = ui.label("00:00:00").classes("text-4xl font-mono text-green-400 mt-2 font-black")

    # ── Section 2 · Throughput Chart + Active Pipeline Status ────────────────
    with ui.row().classes("w-full gap-4 items-stretch"):
        with ui.card().classes("flex-[3] bg-slate-800/30 border-slate-700 p-0 overflow-hidden shadow-2xl"):
            with ui.row().classes("w-full p-4 border-b border-slate-700/50 items-center bg-slate-800/20"):
                ui.icon("show_chart", size="20px").classes("text-blue-400")
                ui.label("Real-time Throughput History").classes("text-sm font-bold text-slate-300")

            chart = ui.echart({
                "xAxis": {"type": "category", "show": False},
                "yAxis": {
                    "type": "value",
                    "splitLine": {"lineStyle": {"color": "#1e293b"}},
                    "axisLabel": {"color": "#64748b"},
                },
                "series": [{
                    "data": [0] * 20,
                    "type": "line",
                    "smooth": True,
                    "areaStyle": {
                        "color": {
                            "type": "linear", "x": 0, "y": 0, "x2": 0, "y2": 1,
                            "colorStops": [
                                {"offset": 0, "color": "rgba(59, 130, 246, 0.4)"},
                                {"offset": 1, "color": "rgba(59, 130, 246, 0)"},
                            ],
                        }
                    },
                    "lineStyle": {"color": "#3b82f6", "width": 4},
                    "itemStyle": {"opacity": 0},
                }],
                "grid": {"left": "3%", "right": "3%", "bottom": "5%", "top": "10%", "containLabel": True},
            }).classes("h-72 w-full")

        with ui.card().classes("flex-1 bg-slate-800/50 border-slate-700 p-0 shadow-xl min-w-[300px]"):
            with ui.row().classes("w-full p-4 border-b border-slate-700/50 items-center bg-slate-800/20"):
                ui.icon("account_tree", size="20px").classes("text-purple-400")
                ui.label("Active Pipeline Segments").classes("text-sm font-bold text-slate-300")

            pipeline_list = ui.list().classes("divide-y divide-slate-700/50 w-full")

    # ── Section 2.5 · DAG Visualization ──────────────────────────────────────
    with ui.row().classes("w-full"):
        with ui.card().classes("w-full bg-slate-800/30 border-slate-700 p-0 overflow-hidden shadow-2xl min-h-[300px]"):
            with ui.row().classes("w-full p-4 border-b border-slate-700/50 items-center justify-between bg-slate-800/20"):
                with ui.row().classes("items-center gap-2"):
                    ui.icon("account_tree", size="20px").classes("text-green-400")
                    ui.label("Job Execution DAG (Logical)").classes("text-sm font-bold text-slate-300")
                ui.button(icon="refresh", on_click=lambda: update_metrics()).props("flat dense").classes("text-slate-500")

            with ui.scroll_area().classes("w-full h-80 bg-[#0f172a]/50 p-4"):
                with ui.column().classes("w-full items-center"):
                    dag_view = ui.mermaid("graph LR\n  A[Idle] --> B[Ready]").classes("w-full min-h-[200px]")

    # ── Section 3 · K8s Discovery ─────────────────────────────────────────────
    with ui.row().classes("w-full gap-4 items-stretch"):

        # Flink Pods card
        with ui.card().classes("flex-1 bg-slate-800/30 border-slate-700 p-0 shadow-2xl overflow-hidden"):
            with ui.row().classes(
                "w-full p-4 border-b border-slate-700/50 items-center justify-between bg-slate-800/20"
            ):
                with ui.row().classes("items-center gap-2"):
                    ui.icon("dns", size="20px").classes("text-cyan-400")
                    ui.label("Flink Pods").classes("text-sm font-bold text-slate-300")
                ui.badge(
                    "LIVE" if K8S_AVAILABLE else "DEMO",
                    color="green" if K8S_AVAILABLE else "amber",
                ).classes("text-[9px]")

            pods_column = ui.column().classes("w-full")
            with pods_column:
                for pod in discover_flink_pods():
                    _build_pod_row(pod)

        # Flowlet CRs card
        with ui.card().classes("flex-1 bg-slate-800/30 border-slate-700 p-0 shadow-2xl overflow-hidden"):
            with ui.row().classes(
                "w-full p-4 border-b border-slate-700/50 items-center justify-between bg-slate-800/20"
            ):
                with ui.row().classes("items-center gap-2"):
                    ui.icon("widgets", size="20px").classes("text-indigo-400")
                    ui.label("Flowlet CRs").classes("text-sm font-bold text-slate-300")
                ui.badge(
                    "LIVE" if K8S_AVAILABLE else "DEMO",
                    color="green" if K8S_AVAILABLE else "amber",
                ).classes("text-[9px]")

            flowlets_column = ui.column().classes("w-full")
            with flowlets_column:
                for fl in discover_flowlets():
                    _build_flowlet_row(fl)

    # ── Section 4 · Log Streaming Terminal ───────────────────────────────────
    with ui.card().classes("w-full bg-black/60 backdrop-blur-xl border-slate-800 h-96 shadow-2xl overflow-hidden"):
        with ui.row().classes("w-full p-2 px-4 border-b border-slate-800 items-center justify-between"):
            with ui.row().classes("items-center gap-4"):
                with ui.row().classes("items-center gap-2"):
                    ui.icon("terminal", size="16px").classes("text-slate-500")
                    ui.label("POD LOGS").classes("text-[10px] font-bold text-slate-500 tracking-widest")
                
                # We'll create the selector AFTER log_view to avoid NameError in callback
                pod_selector_container = ui.row().classes("items-center")

            with ui.row().classes("items-center gap-2"):
                ui.button(icon="clear_all", on_click=lambda: log_view.clear()).props("flat dense").classes("text-slate-600")
                ui.button(icon="download").props("flat dense").classes("text-slate-600")

        log_view = ui.log().classes("w-full h-full text-[11px] font-mono p-4 text-emerald-400/80 bg-black/20")
        log_view.is_streaming = True
        
        # Now create the selector inside its container
        with pod_selector_container:
            pod_options = {p['name']: f"{p['name']} ({p['role']})" for p in discover_flink_pods()}
            def on_pod_change(e):
                pname = e.value
                for p in discover_flink_pods():
                    if p['name'] == pname:
                        start_log_stream(pname, p['namespace'], log_view)
                        break

            pod_selector = ui.select(
                options=pod_options,
                label="Select Pod",
                on_change=on_pod_change
            ).props('dark dense outlined').classes('w-64 text-xs h-8')
            
            # Auto-select first TaskManager if available
            tm_pods = [p for p in discover_flink_pods() if p['role'] == 'TASKMANAGER']
            if tm_pods:
                pod_selector.value = tm_pods[0]['name']
        
        _ts = datetime.now().strftime("%H:%M:%S")
        log_view.push(f"[{_ts}] INFO  Initializing Flinkflow Monitoring System...")


# ─── Periodic Callbacks ───────────────────────────────────────────────────────

def update_metrics():
    # 1. Fetch Real Flink Metrics
    jobs = fetch_flink_jobs()
    running_jobs = [j for j in jobs if j.get("status") == "RUNNING"]
    
    current_throughput = 0
    current_uptime_str = "00:00:00"
    
    if running_jobs:
        jid = running_jobs[0]["id"]
        details = get_job_details(jid)
        if details:
            # Job Uptime
            duration_ms = details.get("duration", 0)
            seconds = duration_ms // 1000
            current_uptime_str = time.strftime('%H:%M:%S', time.gmtime(seconds))
            
            # Throughput (sum over vertices)
            for vertex in details.get("vertices", []):
                vmetrics = get_job_metrics(jid, vertex["id"])
                # Sum up throughput from various possible metric names
                metric_keys = [
                    "0.numRecordsOutPerSecond",
                    "0.Map.numRecordsOutPerSecond",
                    "0.Source__DataGen_Source.numRecordsOutPerSecond",
                    "0.Sink__Writer.numRecordsOutPerSecond"
                ]
                val = max([vmetrics.get(k, 0) for k in metric_keys])
                current_throughput += val
                
            # Update Active Pipeline Segments Panel
            pipeline_list.clear()
            with pipeline_list:
                for vertex in details.get("vertices", []):
                    # Guess type from name
                    vname = vertex["name"].lower()
                    vcolor = "blue"
                    if "source" in vname: vcolor = "green"
                    if "sink" in vname: vcolor = "purple"
                    
                    vstatus = vertex["status"]
                    with ui.item().classes("py-4 px-5 hover:bg-slate-700/30 transition-colors"):
                        with ui.item_section():
                            ui.label(vertex["name"]).classes("text-xs font-bold text-slate-100 truncate w-40")
                            ui.label(vstatus).classes("text-[9px] text-slate-500 tracking-widest")
                        with ui.item_section().props("side"):
                            with ui.column().classes("items-end gap-1"):
                                ui.icon("circle", color="green" if vstatus == "RUNNING" else "amber").classes("text-[8px]")
                                ui.label(f"p={vertex['parallelism']}").classes("text-[10px] text-slate-400 font-mono")
    else:
        # Fallback to random demo data if no running jobs
        current_throughput = random.randint(300, 800)
        current_uptime_str = str(datetime.now() - start_time).split(".")[0]
        
        # Reset if no jobs
        if not running_jobs and fetched_any_job_before:
             pipeline_list.clear()
             with pipeline_list:
                ui.label("No Running Jobs Found").classes("p-4 text-slate-500 italic text-sm")

    # Update Global UI Labels
    throughput_label.set_text(f"{int(current_throughput)} rec/s")
    latency_label.set_text(f"{random.randint(5, 15)} ms") # Latency remains mock as it needs markers
    uptime_label.set_text(current_uptime_str)

    chart.options["series"][0]["data"].append(current_throughput)
    if len(chart.options["series"][0]["data"]) > 50: # Longer history
        chart.options["series"][0]["data"].pop(0)
    chart.update()

    # Update DAG if job changed
    global current_job_id
    if running_jobs:
        new_jid = running_jobs[0]["id"]
        if new_jid != current_job_id:
            current_job_id = new_jid
            plan = get_job_plan(new_jid)
            if plan:
                dag_str = generate_mermaid_from_plan(plan)
                dag_view.set_content(dag_str)
    elif current_job_id:
        current_job_id = None
        dag_view.set_content("graph LR\n  Status[No Active Job]:::transform")

fetched_any_job_before = False
current_job_id = None

def refresh_k8s_discovery():
    pods = discover_flink_pods()
    flowlets = discover_flowlets()

    pods_column.clear()
    with pods_column:
        for pod in pods:
            _build_pod_row(pod)

    flowlets_column.clear()
    with flowlets_column:
        for fl in flowlets:
            _build_flowlet_row(fl)

    log_view.push(
        f"[{datetime.now().strftime('%H:%M:%S')}] INFO  "
        f"K8s refresh — {len(pods)} pod(s), {len(flowlets)} Flowlet CR(s) discovered."
    )


ui.timer(2.0, update_metrics) # Slightly slower for REST API stability
ui.timer(30.0, refresh_k8s_discovery)
ui.timer(0.5, lambda: consume_log_queue(log_view)) # Safely pull logs from queue


# ─── Run ──────────────────────────────────────────────────────────────────────

if __name__ in {"__main__", "__mp_main__"}:
    ui.run(
        title="Flinkflow Monitor",
        dark=True,
        port=int(os.environ.get("PORT", "8082")),
        host="0.0.0.0",
        show=False,
        favicon="📊",
    )
