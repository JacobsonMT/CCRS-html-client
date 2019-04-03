$(document).ready(function () {
    queueTable = $('.job-table').DataTable({
        "paging": true,
        "searching": false,
        "info": false,
        "order": [],
        "lengthMenu": [ [5, 10, 25, 50, -1], [5, 10, 25, 50, "All"] ],
        "stateSave": true,
        "stateSaveParams": function (settings, data) {
            data.order = "";
        },
        "columnDefs": [
            { "orderable": false, "targets": [0, 5] }
        ]
    });

    connect();

});

function deleteJob(e, jobId) {
    var ajaxCall = new XMLHttpRequest();
    ajaxCall.open('GET', "job/"+ jobId + "/delete");
    ajaxCall.send();
    queueTable.row( $(e.target).parents('tr') ).remove().draw();
}

function connect() {
        var source = new EventSource('/register');
        // Handle correct opening of connection
        source.addEventListener('open', function (e) {
            console.log('Connected.');
        });

        // Update the state when ever a message is sent
        source.addEventListener('message', function (e) {
            updateJobTable();
        }, false);
        // Reconnect if the connection fails
        source.addEventListener('error', function (e) {
            console.log('Disconnected.');
            if (e.readyState == EventSource.CLOSED) {
                connected = false;
                connect();
            }
        }, false);
}

function updateJobTable() {
    $.get("job-table", function(fragment) { // get from controller
        $("#job-table").replaceWith(fragment); // update snippet of page
        queueTable = $('.job-table').DataTable({
            "paging": true,
            "searching": false,
            "info": false,
            "order": [],
            "lengthMenu": [ [5, 10, 25, 50, -1], [5, 10, 25, 50, "All"] ],
            "stateSave": true,
            "stateSaveParams": function (settings, data) {
                data.order = "";
            },
            "columnDefs": [
                { "orderable": false, "targets": [0, 5] }
            ]
        });
    });

}