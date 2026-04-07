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


import os
import requests

FLINK_BASE_URL = os.environ.get("FLINK_BASE_URL", "http://localhost:8081")

def fetch_flink_overview():
    try:
        resp = requests.get(f"{FLINK_BASE_URL}/overview", timeout=2)
        if resp.status_code == 200:
            return resp.json()
    except:
        pass
    return None

def fetch_flink_jobs():
    try:
        resp = requests.get(f"{FLINK_BASE_URL}/jobs", timeout=2)
        if resp.status_code == 200:
            return resp.json().get("jobs", [])
    except:
        pass
    return []

def get_job_metrics(jid, vid):
    try:
        # Try a few common metric IDs
        metrics_to_get = "0.numRecordsOutPerSecond,0.numRecordsInPerSecond,0.Sink__Writer.numRecordsOutPerSecond,0.Map.numRecordsOutPerSecond,0.Source__DataGen_Source.numRecordsOutPerSecond"
        resp = requests.get(f"{FLINK_BASE_URL}/jobs/{jid}/vertices/{vid}/metrics?get={metrics_to_get}", timeout=2)
        if resp.status_code == 200:
            results = resp.json()
            metrics = {}
            for m in results:
                metrics[m["id"]] = float(m["value"])
            return metrics
    except:
        pass
    return {}

def get_job_details(jid):
    try:
        resp = requests.get(f"{FLINK_BASE_URL}/jobs/{jid}", timeout=2)
        if resp.status_code == 200:
            return resp.json()
    except:
        pass
    return None

def get_job_plan(jid):
    try:
        resp = requests.get(f"{FLINK_BASE_URL}/jobs/{jid}/plan", timeout=2)
        if resp.status_code == 200:
            return resp.json().get("plan")
    except:
        pass
    return None
