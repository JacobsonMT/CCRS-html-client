$(document).ready(function () {
    queueTable = $('.job-table').DataTable({
        "paging": true,
        "searching": false,
        "info": false,
        "order": [],
        "lengthMenu": [ [5, 10, 25, 50, -1], [5, 10, 25, 50, "All"] ],
        "stateSave": true,
        "columnDefs": [
            { "orderable": false, "targets": [0, 5] }
        ]
    });
});

function deleteJob(e, jobId) {
    var ajaxCall = new XMLHttpRequest();
    ajaxCall.open('GET', "job/"+ jobId + "/delete");
    ajaxCall.send();
    queueTable.row( $(e.target).parents('tr') ).remove().draw();
}