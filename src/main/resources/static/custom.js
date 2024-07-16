document.addEventListener('DOMContentLoaded', function() {
    const selectedTables = JSON.parse(localStorage.getItem('selectedTables'));

    fetch(`/api/columns?tableNames=${selectedTables.join(',')}`)
        .then(response => response.json())
        .then(data => {
            const columnsList = document.getElementById('columnsList');
            columnsList.innerHTML = ''; // Wyczyść listę przed dodaniem nowych elementów

            for (const [table, columns] of Object.entries(data)) {
                const tableHeader = document.createElement('h2');
                tableHeader.textContent = table;
                columnsList.appendChild(tableHeader);

                const ul = document.createElement('ul');
                columns.forEach(column => {
                    const li = document.createElement('li');
                    const checkbox = document.createElement('input');
                    checkbox.type = 'checkbox';
                    checkbox.id = column;
                    checkbox.name = 'columns';
                    checkbox.value = `${table}.${column}`;

                    const label = document.createElement('label');
                    label.htmlFor = column;
                    label.textContent = column;

                    li.appendChild(checkbox);
                    li.appendChild(label);
                    ul.appendChild(li);
                });
                columnsList.appendChild(ul);
            }
        })
        .catch(error => console.error('Error fetching columns:', error));

    document.getElementById('exportCustomTables').addEventListener('click', function() {
        const selectedColumns = [];
        document.querySelectorAll('input[name="columns"]:checked').forEach(checkbox => {
            selectedColumns.push(checkbox.value);
        });

        if (selectedColumns.length > 0) {
            fetch('/api/export', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(selectedColumns)
            })
                .then(response => response.text())
                .then(taskId => {
                    console.log('Eksport rozpoczęty. ID zadania:', taskId);
                    alert('Eksport rozpoczęty. ID zadania: ' + taskId);

                    // Pobierz plik po rozpoczęciu eksportu
                    fetch(`/api/excel/${taskId}`)
                        .then(response => response.blob())
                        .then(blob => {
                            const url = window.URL.createObjectURL(blob);
                            const a = document.createElement('a');
                            a.href = url;
                            a.download = `export-${taskId}.xlsx`;
                            document.body.appendChild(a);
                            a.click();
                            a.remove();
                        })
                        .catch(error => console.error('Error downloading Excel file:', error));
                })
                .catch(error => console.error('Error exporting tables:', error));
        } else {
            alert('Proszę zaznaczyć co najmniej jedną kolumnę do eksportu.');
        }
    });
});
