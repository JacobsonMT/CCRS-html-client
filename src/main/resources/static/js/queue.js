$(document).ready(function () {
    queueTable = initJobTable();

    pollUntilDone(5000, 0);

});

// create a promise that resolves after a short delay
function delay(t) {
    return new Promise(function(resolve) {
        setTimeout(resolve, t);
    });
}

function pollUntilDone(interval, timeout) {
    let start = Date.now();
    let previousPending = -1;
    function run() {
        return $.get("/pending").then(function(pending) {
            if (pending < previousPending) {
                updateJobTable();
                previousPending = pending;
                return delay(interval).then(run);
            } else if ( pending <= 0 ) {
                // done
                return pending;
            } else {
                if (timeout !== 0 && Date.now() - start > timeout) {
                    throw new Error("timeout error on pollUntilDone");
                } else {
                    // run again with a short delay
                    previousPending = pending;
                    return delay(interval).then(run);
                }
            }
        });
    }
    return run();
}

function deleteJob(e, jobId) {
    var ajaxCall = new XMLHttpRequest();
    ajaxCall.open('GET', "job/"+ jobId + "/delete");
    ajaxCall.send();
    queueTable.row( $(e.target).parents('tr') ).remove().draw();
}

function updateJobTable() {
    $.get("job-table", function(fragment) { // get from controller
        $("#job-table").replaceWith(fragment); // update snippet of page
        queueTable = initJobTable();
    });

}

function initJobTable() {
    return $('.job-table').DataTable({
        "paging": true,
        "searching": false,
        "info": false,
        "order": [],
        "lengthMenu": [ [5, 10, 25, 50, -1], [5, 10, 25, 50, "All"] ],
        "stateSave": true,
        "stateSaveCallback": function(settings,data) {
            localStorage.setItem( 'DataTable', JSON.stringify(data) )
        },
        "stateLoadCallback": function(settings) {
            return JSON.parse( localStorage.getItem( 'DataTable' ) )
        },
        "stateSaveParams": function (settings, data) {
            data.order = "";
        },
        "columnDefs": [
            { "orderable": false, "targets": [0, -1] }
        ]
    });
}