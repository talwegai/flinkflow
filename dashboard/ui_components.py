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

def _phase_color(phase: str) -> tuple:
    """Returns (nicegui_color, unused_hex) for a pod phase."""
    return {
        "Running":   ("green",  "#14532d"),
        "Pending":   ("amber",  "#451a03"),
        "Succeeded": ("blue",   "#1e3a5f"),
        "Failed":    ("red",    "#450a0a"),
        "Unknown":   ("grey",   "#1e293b"),
    }.get(phase, ("grey", "#1e293b"))


def _type_color(ftype: str) -> str:
    return {"source": "blue", "sink": "purple", "action": "amber"}.get(
        ftype.lower(), "grey"
    )

def generate_mermaid_from_plan(plan_data):
    if not plan_data:
        return "graph LR\n  NoJob[No Active Job]"
    
    nodes = plan_data.get("nodes", [])
    output = "graph LR\n"
    
    # Styles
    output += "  classDef source fill:#166534,stroke:#22c55e,color:#fff;\n"
    output += "  classDef sink fill:#581c87,stroke:#a855f7,color:#fff;\n"
    output += "  classDef transform fill:#1e3a8a,stroke:#3b82f6,color:#fff;\n"
    
    # Node Map
    node_map = {}
    
    for node in nodes:
        raw_id = str(node.get("id", "unknown"))
        node_id = raw_id.replace("-", "_")
        desc = node.get("description", "Unknown")
        
        # Parse description if chained
        steps = desc.replace("+- ", "").replace("   ", "").split("<br/>")
        steps = [s.strip().replace("\\n", " ").replace("\\\"", "") for s in steps if s.strip()]
        
        if not steps:
            steps = [node.get("name", "Unknown Operator")]
            
        prev_sub_id = None
        for i, step in enumerate(steps):
            sub_id = f"dag_node_{node_id}_{i}"
            
            # Type identification
            cls = "transform"
            ls = step.lower()
            if "source" in ls: cls = "source"
            elif any(x in ls for x in ["sink", "print", "writer", "file", "kafka", "jdbc"]): cls = "sink"
            
            # Very clean step name for Mermaid
            clean_step = step.replace("[", "(").replace("]", ")").replace("(", " ").replace(")", " ").replace("\"", "'")
            
            output += f"  {sub_id}[\"{clean_step}\"]:::{cls};\n"
            if prev_sub_id:
                output += f"  {prev_sub_id} --> {sub_id};\n"
            
            if i == 0:
                node_map[raw_id] = {"first": sub_id}
            node_map[raw_id]["last"] = sub_id
            prev_sub_id = sub_id
            
    # Draw connections
    for node in nodes:
        curr_id = node.get("id")
        for inp in node.get("inputs", []):
            src_id = inp.get("id")
            if src_id in node_map and curr_id in node_map:
                output += f"  {node_map[src_id]['last']} --> {node_map[curr_id]['first']};\n"
    
    return output

def _build_pod_row(pod: dict):
    phase = pod.get("phase", "Unknown")
    icon_color, _ = _phase_color(phase)
    phase_css = {
        "Running": "text-green-400", "Pending": "text-amber-400",
        "Failed":  "text-red-400",   "Succeeded": "text-blue-400",
    }.get(phase, "text-slate-400")
    role = pod.get("role", "UNKNOWN")
    role_css = {
        "JOBMANAGER": "text-cyan-400", "TASKMANAGER": "text-blue-400",
        "OPERATOR":   "text-indigo-400",
    }.get(role, "text-slate-400")

    with ui.row().classes("w-full px-4 py-3 hover:bg-slate-700/20 transition-colors items-center gap-3"):
        ui.icon("circle", color=icon_color).classes("text-[8px] shrink-0")
        with ui.column().classes("gap-0 flex-1 min-w-0"):
            ui.label(pod["name"]).classes("text-[11px] font-mono text-slate-200 truncate")
            ui.label(pod.get("namespace", "")).classes("text-[9px] text-slate-500")
        with ui.row().classes("items-center gap-3 shrink-0"):
            ui.label(role).classes(f"text-[9px] font-bold tracking-widest {role_css}")
            ui.label(phase).classes(f"text-[10px] font-mono {phase_css}")
            ui.label(pod.get("age", "—")).classes("text-[9px] text-slate-500 font-mono w-8 text-right")


def show_flowlet_details(fl: dict):
    with ui.dialog() as dialog, ui.card().classes('w-full max-w-2xl bg-slate-900 border border-slate-700 shadow-2xl p-6'):
        with ui.row().classes('w-full items-center justify-between border-b border-slate-800 pb-4 mb-4'):
            with ui.row().classes('items-center gap-3'):
                color = _type_color(fl.get("type", "unknown"))
                ui.icon("extension", color=color).classes("text-2xl")
                ui.label(fl.get("title", fl["name"])).classes('text-xl font-black text-white tracking-tight')
            ui.button(icon='close', on_click=dialog.close).props('flat round dense').classes('text-slate-400 hover:text-white')
            
        with ui.scroll_area().classes('w-full max-h-[60vh] pr-4'):
            ui.label('DESCRIPTION').classes('text-[10px] font-bold text-slate-500 tracking-widest mb-2')
            ui.markdown(fl.get('description', 'No description provided.')).classes('text-sm text-slate-300 mb-6 leading-relaxed')
            
            params = fl.get('parameters', {})
            if params:
                ui.label('PARAMETERS').classes('text-[10px] font-bold text-slate-500 tracking-widest mb-3')
                with ui.column().classes('w-full gap-3'):
                    for p_name, p_spec in params.items():
                        with ui.card().classes('w-full bg-slate-800/40 border border-slate-700/50 p-4 shadow-none'):
                            with ui.row().classes('items-center gap-2 mb-2'):
                                ui.label(p_name).classes('text-sm font-mono font-bold text-blue-400')
                                if p_spec.get('required'):
                                    ui.badge('REQUIRED', color='red-500').classes('text-[9px] px-1.5 py-0.5 font-bold')
                                ui.label(f"type: {p_spec.get('type', 'any')}").classes('text-[10px] text-slate-500 font-mono')
                                if p_spec.get('title'):
                                    ui.label(f"• {p_spec.get('title')}").classes('text-[11px] text-slate-400')
                            ui.label(p_spec.get('description', '')).classes('text-xs text-slate-300')
                            if 'defaultValue' in p_spec:
                                ui.label(f"Default: {p_spec.get('defaultValue')}").classes('text-[10px] text-slate-500 mt-2 font-mono bg-slate-900/50 px-2 py-1 rounded inline-block')
            else:
                ui.label('No parameters defined.').classes('text-sm text-slate-500 italic')
            
    dialog.open()

def _build_flowlet_row(fl: dict):
    ftype = fl.get("type", "unknown")
    color = _type_color(ftype)
    type_css = {
        "source": "text-blue-400", "sink": "text-purple-400", "action": "text-amber-400",
    }.get(ftype.lower(), "text-slate-400")

    with ui.row().classes("group w-full px-4 py-3 hover:bg-slate-700/40 transition-colors items-center gap-3 cursor-pointer").on('click', lambda: show_flowlet_details(fl)):
        ui.icon("extension", color=color).classes("text-[14px] shrink-0")
        with ui.column().classes("gap-0 flex-1 min-w-0"):
            ui.label(fl.get("title", fl["name"])).classes("text-[11px] font-bold text-slate-200 truncate group-hover:text-white transition-colors")
            ui.label(fl.get("name", "")).classes("text-[9px] font-mono text-slate-500 truncate")
        with ui.row().classes("items-center gap-3 shrink-0"):
            ui.label(ftype.upper()).classes(f"text-[9px] font-bold tracking-widest {type_css}")
            ui.label(f"v{fl.get('version', '—')}").classes("text-[9px] text-slate-500 font-mono")
            ui.icon("chevron_right", size="16px").classes("text-slate-600 group-hover:text-slate-300 transition-colors")
