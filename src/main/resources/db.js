var rows, rowsOriginal;

function $(id) {
	return document.querySelector(id);
}

function $all(id) {
	return document.querySelectorAll(id);
}

function keyupSql(e) {
	if (e.keyCode === 13) {
		execSql($('#txtSql').value);
	}
	return false;
}

function sortBy(column, reverse) {
	$('#result').innerHTML = '';
	rows.sort(function(a, b) {
	    return reverse ? a[column].localeCompare(b[column]) : b[column].localeCompare(a[column]);
	});
	createTable();
}

function showFilter(column) {
	var filterInTh = $all('#tblSql th .filter')[column];
	var classes = filterInTh.className;
	filterInTh.className = (classes.includes('vHidden')) ? classes.replace(' vHidden', '') : classes + ' vHidden';
}

function filterChanged(value, col) {
	var now = parseInt(performance.now());
	rows = rowsOriginal.filter(row => { return row[col].includes(value); });
	createTable();
	showRowCount(now, rows.length);
}

function execSql(sql) {
	var t1ms = parseInt(window.performance.now());
	$('#loading').style.display = 'inline-block';  // show
	fetch('select.json', {
		method : 'post',
		body : sql
	}).then(function(response) {
		if (response.status === 400) { // Bad Request
			response.text().then(function(data) {
			  $('#txtError').innerHTML += ': ' + data;
			});
		  showError('Cannot parse SQL');
		} else {
			response.json().then(function(data) {
			  $('#loading').style.display = '';  // hide
				rows = data;
				rowsOriginal = JSON.parse(JSON.stringify(rows));
				if (rows.length === 0) {
					showNoResults();
				} else {
					createTable();
					showRowCount(t1ms, rows.length);
				}
			});
		}
	}).catch(function(error) {
		showError(error);
	});
}

function showError(error) {
	$('#result').innerHTML = '';
	$('#resultCount').innerHTML = '';
	$('#noResults').className = 'hidden';
	$('#txtError').innerHTML = error;
	$('#loading').style.display = '';
}

function showNoResults() {
	$('#result').innerHTML = '';
	$('#resultCount').innerHTML = '';
	$('#noResults').className = '';
	$('#txtError').innerHTML = '';
}

function createTable() {
	var tbl = '<table id="tblSql">\n  <thead>%h</thead>\n  <tbody>%b</tbody>\n</table>';
	var th = '<th>%s <input type="text" class="filter vHidden" onchange="filterChanged(this.value, \'%c\')"> </th>';
	var td = '<td>%s</td>';
	
	var headers = Object.keys(rows[0]);
	var row = '', i = 0;
	var thead = '', tbody = '';
	headers.forEach(h => {
		row += th.replace('%s', h + addSortingInTableHeader(h, i++)).replace('%c', h);
	});
	thead = `<tr>${row}</tr>`;
	
	rows.forEach(function(r) { 
		row = '';
		headers.forEach(h => { row += td.replace('%s', r[h]); });
		tbody += `<tr>${row}</tr>`;
	});
	$('#txtError').innerHTML = '';
	$('#noResults').className = 'hidden';
	$('#result').innerHTML = tbl.replace('%h', thead).replace('%b', tbody);
}

function addSortingInTableHeader(col, idx) {
	return  `<span onclick="sortBy('${col}', false)">↑</span>` +
			`<span onclick="sortBy('${col}', true)">↓</span>` +
			`<span onclick="showFilter(${idx})">⌕</span>`;
}

function showRowCount(t1ms, count) {
	var duration = parseInt(window.performance.now() ) - t1ms;
	$('#resultCount').innerHTML = `Showing ${count} results. Time: ${duration} ms`;
}