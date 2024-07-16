document.getElementById('fetchTables').addEventListener('click', function() {
    fetch('/api/tables')
        .then(response => response.json())
        .then(data => {
            const tableList = document.getElementById('tableList');
            tableList.innerHTML = '';
            data.forEach(table => {
                const li = document.createElement('li');
                li.textContent = table;
                tableList.appendChild(li);
            });
        })
        .catch(error => console.log('Error fetching tables: ', error));

});