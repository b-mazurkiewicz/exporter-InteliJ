document.addEventListener('DOMContentLoaded', function() {
    const tableList = document.getElementById('tableList');
    const exportTablesBtn = document.getElementById('exportTables');
    const exportSchemasBtn = document.getElementById('exportSchemas');
    const statusDiv = document.getElementById('status');

    // Function to display status messages
    function displayStatus(message, type) {
        statusDiv.innerHTML = ''; // Clear previous status messages
        const p = document.createElement('p');
        p.className = type;
        p.textContent = message;
        statusDiv.appendChild(p);
    }

    // Fetch and display tables
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

    // Export selected tables
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

                    // Download the Excel file once export starts
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

    // Export database schemas
    exportSchemasBtn.addEventListener('click', function() {
        const schemaFile = document.getElementById('schemaFile').files[0];
        if (!schemaFile) {
            alert('Please select an Excel file to upload.');
            return;
        }

        const formData = new FormData();
        formData.append('file', schemaFile);

        fetch('/api/schema/upload', {  // Ensure correct endpoint
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                console.log('Schema upload successful:', data);
                displayStatus('Schema upload successful. Task ID: ' + data.taskId, 'success');

                // Optionally, start exporting schema directly after upload
                downloadExcelFile(data.taskId); // Use data.taskId
            })
            .catch(error => {
                console.error('Error uploading schema:', error);
                displayStatus('Error uploading schema: ' + error.message, 'error');
            });
    });

    // Function to download Excel file after exporting tables
    function downloadExcelFile(taskId) {
        displayStatus('Downloading Excel file...', 'info');
        fetch(`/api/excel/${taskId}`) // Updated endpoint
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
                window.URL.revokeObjectURL(url); // Clean up the URL object
                displayStatus('File download initiated', 'success');
            })
            .catch(error => {
                console.error('Error downloading Excel file:', error);
                displayStatus('Error downloading Excel file: ' + error.message, 'error');
            });
    }
});
