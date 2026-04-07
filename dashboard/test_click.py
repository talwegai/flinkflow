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

def on_click(e):
    print("Row clicked!")
    with ui.dialog() as dialog, ui.card():
        ui.label('Detailed info')
        ui.button('Close', on_click=dialog.close)
    dialog.open()

with ui.row().classes('cursor-pointer bg-blue-500 p-4').on('click', on_click):
    ui.label('Click me')

ui.run(port=8083, show=False)
