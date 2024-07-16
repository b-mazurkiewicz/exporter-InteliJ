document.addEventListener('DOMContentLoaded', function() {
    fetch('/api/tables')
        .then(response => response.json())
        .then(data => {
            const tableList = document.getElementById('tableList');
            tableList.innerHTML = ''; // Wyczyść listę przed dodaniem nowych elementów
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
        .catch(error => console.error('Error fetching tables:', error));

    document.getElementById('exportTables').addEventListener('click', function() {
        const selectedTables = [];
        document.querySelectorAll('input[name="tables"]:checked').forEach(checkbox => {
            selectedTables.push(checkbox.value);
        });

        if (selectedTables.length > 0) {
            fetch('/api/export', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(selectedTables)
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
            alert('Proszę zaznaczyć co najmniej jedną tabelę do eksportu.');
        }
    });
});
