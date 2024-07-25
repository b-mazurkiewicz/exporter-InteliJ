document.addEventListener('DOMContentLoaded', function() {
    const tableList = document.getElementById('tableList');
    const exportTablesBtn = document.getElementById('exportTables');
    const exportSchemasBtn = document.getElementById('exportSchemas');
    const schemaList = document.getElementById('schemaList');
    const exportSelectedSchemasBtn = document.getElementById('exportSelectedSchemas');
    const statusDiv = document.getElementById('status');

    function displayStatus(message, type) {
        statusDiv.innerHTML = ''; // Clear previous status messages
        const p = document.createElement('p');
        p.className = type; // Assign class based on message type (success, error, info)
        p.textContent = message;
        statusDiv.appendChild(p);
    }

    function fetchSchemas() {
        fetch('/files')
            .then(response => response.json())
            .then(data => {
                schemaList.innerHTML = ''; // Clear previous list items
                data.forEach(schema => {
                    console.log(`Fetched schema:`, schema); // Log schema data for debugging
                    const li = document.createElement('li');
                    const checkbox = document.createElement('input');
                    checkbox.type = 'checkbox';
                    checkbox.id = schema.name;
                    checkbox.name = 'schemas';
                    checkbox.value = schema.url; // Assuming schema.url contains the full URL

                    const label = document.createElement('label');
                    label.htmlFor = schema.name;
                    label.textContent = schema.name;

                    const closeButton = document.createElement('button');
                    closeButton.className = 'close-btn';
                    closeButton.textContent = 'X';
                    closeButton.addEventListener('click', function() {
                        console.log(`Attempting to delete schema with ID: ${schema.id}`); // Log ID
                        deleteSchema(schema.id, li); // Pass schema id and li element
                    });

                    li.appendChild(checkbox);
                    li.appendChild(label);
                    li.appendChild(closeButton);
                    schemaList.appendChild(li);
                });
            })
            .catch(error => {
                console.error('Error fetching schemas:', error);
                displayStatus('Error fetching schemas: ' + error.message, 'error');
            });
    }

    fetch('/api/tables')
        .then(response => response.json())
        .then(data => {
            tableList.innerHTML = ''; // Clear previous list items
            data.forEach(table => {
                const li = document.createElement('li');
                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.id = table;
                checkbox.name = 'tables';
                checkbox.value = table;

                const label = document.createElement('label');
                label.htmlFor = table;
                label.textContent = table;

                li.appendChild(checkbox);
                li.appendChild(label);
                tableList.appendChild(li);
            });
        })
        .catch(error => {
            console.error('Error fetching tables:', error);
            displayStatus('Error fetching tables: ' + error.message, 'error');
        });

    fetchSchemas(); // Fetch schemas initially

    exportTablesBtn.addEventListener('click', function() {
        const selectedTables = [];
        document.querySelectorAll('input[name="tables"]:checked').forEach(checkbox => {
            selectedTables.push(checkbox.value);
        });

        if (selectedTables.length > 0) {
            displayStatus('Starting export of selected tables...', 'info');
            fetch('/api/export', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(selectedTables)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.text();
                })
                .then(taskId => {
                    console.log('Export started. Task ID:', taskId);
                    displayStatus(`Export started. Task ID: ${taskId}`, 'success');
                    alert(`Export started. Task ID: ${taskId}`); // Here is the alert

                    downloadExcelFile(taskId);
                })
                .catch(error => {
                    console.error('Error exporting tables:', error);
                    displayStatus('Error exporting tables: ' + error.message, 'error');
                });
        } else {
            alert('Please select at least one table to export.');
        }
    });

    exportSchemasBtn.addEventListener('click', function() {
        const schemaFile = document.getElementById('schemaFile').files[0];
        if (!schemaFile) {
            alert('Please select an Excel file to upload.');
            return;
        }

        const formData = new FormData();
        formData.append('file', schemaFile);

        displayStatus('Uploading schema...', 'info');
        fetch('/api/schema/upload', {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                console.log('Schema upload successful:', data);
                displayStatus('Schema upload successful. Task ID: ' + data.taskId, 'success');
                alert(`Schema upload successful. Task ID: ${data.taskId}`); // Alert message

                downloadSchemaFile(data.taskId);
            })
            .catch(error => {
                console.error('Error uploading schema:', error);
                displayStatus('Error uploading schema: ' + error.message, 'error');
            });
    });

    exportSelectedSchemasBtn.addEventListener('click', function() {
        const selectedSchemas = [];
        document.querySelectorAll('input[name="schemas"]:checked').forEach(checkbox => {
            selectedSchemas.push(checkbox.value);
        });

        if (selectedSchemas.length > 0) {
            displayStatus('Starting export of selected schemas...', 'info');
            selectedSchemas.forEach(schemaUrl => {
                const fileId = schemaUrl.split('/').pop();

                fetch(`/files/${fileId}`, {
                    method: 'POST'
                })
                    .then(response => {
                        if (!response.ok) {
                            throw new Error('Network response was not ok');
                        }
                        return response.blob();
                    })
                    .then(blob => {
                        const url = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `schema-${fileId}.xlsx`;
                        document.body.appendChild(a);
                        a.click();
                        a.remove();
                        window.URL.revokeObjectURL(url);
                        displayStatus('Schema file download initiated', 'success');
                    })
                    .catch(error => {
                        console.error('Error exporting file:', error);
                        displayStatus('Error exporting file: ' + error.message, 'error');
                    });
            });
        } else {
            alert('Please select at least one schema to export.');
        }
    });

    function downloadExcelFile(taskId) {
        displayStatus('Downloading Excel file...', 'info');
        fetch(`/api/excel/${taskId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.blob();
            })
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `export-${taskId}.xlsx`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                window.URL.revokeObjectURL(url);
                displayStatus('Excel file download initiated', 'success');
            })
            .catch(error => {
                console.error('Error downloading Excel file:', error);
                displayStatus('Error downloading Excel file: ' + error.message, 'error');
            });
    }

    function downloadSchemaFile(taskId) {
        displayStatus('Downloading schema file...', 'info');
        fetch(`/api/schema/${taskId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.blob();
            })
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `schema-${taskId}.xlsx`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                window.URL.revokeObjectURL(url);
                displayStatus('Schema file download initiated', 'success');
            })
            .catch(error => {
                console.error('Error downloading schema file:', error);
                displayStatus('Error downloading schema file: ' + error.message, 'error');
            });
    }

    function deleteSchema(schemaId, listItem) {
        console.log("Attempting to delete schema with ID:", schemaId); // Log ID
        fetch(`/files/${schemaId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`Error ${response.status}: ${text}`);
                    });
                }
                return response.text(); // Read response as text
            })
            .then(data => {
                console.log('Delete response data:', data); // Log response data
                schemaList.removeChild(listItem);
                displayStatus('Schema deleted successfully', 'success');
                fetchSchemas(); // Fetch updated list of schemas
            })
            .catch(error => {
                console.error('Error deleting schema:', error);
                displayStatus('Error deleting schema: ' + error.message, 'error');
            });
    }
});
