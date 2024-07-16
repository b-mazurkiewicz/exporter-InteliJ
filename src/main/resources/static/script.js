document.addEventListener('DOMContentLoaded', function()
{
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
});