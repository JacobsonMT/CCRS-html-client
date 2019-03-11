$(document).ready(function () {
    $('.job-table').DataTable({
        "paging": true,
        "searching": false,
        "info": false,
        "order": [],
        "lengthMenu": [ [5, 10, 25, 50, -1], [5, 10, 25, 50, "All"] ]
    });
});