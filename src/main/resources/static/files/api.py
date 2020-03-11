#!/usr/bin/env python3

"""Functional API for LIST-S2 RESTful web service"""

__author__ = 'mjacobson'

import requests
import glob
import os
import uuid
import json
import sys

BASE_URL = "https://list-s2-api.msl.ubc.ca/api"
SUBMIT_JOB_ENDPOINT = BASE_URL + '/job'
JOB_ENDPOINT = BASE_URL + "/job/{job_id}"
BATCH_ENDPOINT = BASE_URL + "/batch/{batch_id}/jobs"


def submit_jobs_directory(directory, email=""):
    """
    Submit all *.fasta files in given directory as a single batch. Write batch file to active directory.
    :param directory: Path of directory containing FASTA files
    :param email: (Optional) send updates to this email when job is submitted/started/completed
    :return: String containing information about the request
    """
    batch_id = str(uuid.uuid4())

    results = []
    for filename in glob.glob(os.path.join(directory, "*.fasta")):
        with open(filename) as f:
            content = f.read()
        res = submit_job(content, batch_id, email)
        res['filename'] = filename
        results.append(res)

    if len(results) > 0:
        with open(batch_id + ".batch.txt", 'w') as of:
            json.dump(results, of)
            of.write('\n')

    return "{cnt} jobs submitted, see '{batch}'.\nRun 'api.py get -f {batch}' to retrieve job status/results.".format(
        cnt=sum([r['totalRequestedJobs'] for r in results]),
        batch=batch_id + ".batch.txt"
    )


def submit_jobs_file(filename, email=""):
    """
    Submit fasta file as a single batch. Write batch file to active directory.
    :param filename: Path to FASTA file
    :param email: (Optional) send updates to this email when job is submitted/started/completed
    :return: String containing information about the request
    """
    batch_id = str(uuid.uuid4())

    with open(filename) as f:
        content = f.read()
    res = submit_job(content, batch_id, email)
    res['filename'] = filename

    with open(batch_id + ".batch.txt", 'w') as of:
        json.dump([res], of)
        of.write('\n')

    return "{cnt} jobs submitted, see '{batch}'.\nRun 'api.py get -f {batch}' to retrieve job status/results.".format(
        cnt=res['totalRequestedJobs'],
        batch=batch_id + ".batch.txt"
    )


def submit_job(content, batch_id="", email=""):
    """
    Send request to submit job
    :param content: FASTA content of job
    :param batch_id: Batch Id for this job
    :param email: (Optional) send updates to this email when job is submitted/started/completed
    :return: Response JSON
    """
    response = requests.post(SUBMIT_JOB_ENDPOINT, json={
        "email": email,
        "fasta": content,
        "batchId": batch_id
    })
    return response.json()


def get_batch_file(filename, with_results=True):
    """
    Get job results/status for all jobs in a batch file
    :param filename: Path to batch file
    :param with_results: True if completed results should be included in response, else False
    :return: List of JSON objects with job results/status
    """
    batch_ids = get_batch_ids_from_batch_file(filename)
    if batch_ids is None:
        print("Could not parse batch ids from file '{}'.".format(filename))
        return []

    results = []
    for batch_id in batch_ids:
        results += get_batch(batch_id, with_results)

    return results


def get_job(job_id, with_results=True):
    """
    Get job results/status for a single job with given id
    :param job_id: Job Id
    :param with_results: True if completed results should be included in response, else False
    :return: List containing single JSON object with job results/status if job exists, else empty list
    """
    res = requests.get(JOB_ENDPOINT.format(job_id=job_id), params={"withResults": with_results})
    if res.status_code == 404:
        return []
    return [res.json()]


def get_batch(batch_id, with_results=True):
    """
    Get job results/status for a single batch with given id
    :param batch_id: Batch Id
    :param with_results: True if completed results should be included in response, else False
    :return: List of JSON objects with job results/status
    """
    return requests.get(BATCH_ENDPOINT.format(batch_id=batch_id), params={"withResults": with_results}).json()


def delete_batch_file(filename):
    """
    Request stop for all jobs in all batches in a batch file
    :param filename: Path to batch file
    :return: List of response strings one per batch
    """
    batch_ids = get_batch_ids_from_batch_file(filename)
    if batch_ids is None:
        print("Could not parse batch ids from file '{}'.".format(filename))
        return []

    results = []
    for batch_id in batch_ids:
        results.append(delete_batch(batch_id))

    return results


def delete_job(job_id):
    """
    Request stop for a job with given job id
    :param job_id: Job Id
    :return: Response string
    """
    return requests.delete(JOB_ENDPOINT.format(job_id=job_id)).text


def delete_batch(batch_id):
    """
    Request stop for all jobs for a batch with given batch id
    :param batch_id: Batch Id
    :return: Response string
    """
    return requests.delete(BATCH_ENDPOINT.format(batch_id=batch_id)).text


def get_batch_ids_from_batch_file(filename):
    """
    Get all batch ids from a batch file
    :param filename: Path to batch file
    :return: List of batch ids or None if error loading information from batch file
    """
    try:
        with open(filename) as f:
            results = json.load(f)
    except ValueError:
        print("Unknown file format, verify file contains batch information '<batch_id>.batch.txt'")
        return None

    batch_ids = set([res['batchId'] for res in results])
    if len(batch_ids) > 1:
        print("Multiple batch ids found in batch file '{}'.".format(filename))

    return batch_ids


if __name__ == "__main__":
    import argparse


    class HelpParser(argparse.ArgumentParser):
        def error(self, message):
            sys.stderr.write('error: %s\n\n' % message)
            self.print_help()
            sys.exit(2)


    def dir_path(path):
        """
        Argparse type for directories
        """
        if os.path.isdir(path):
            return path
        else:
            raise argparse.ArgumentTypeError("readable_dir: '{path}' is not a valid path".format(path=path))


    def file_path(path):
        """
        Argparse type for files
        """
        if os.path.isfile(path):
            return path
        else:
            raise argparse.ArgumentTypeError("readable_file: '{path}' is not a valid file".format(path=path))


    parser = HelpParser(description="Functional API for LIST-S2 RESTful web service")
    subparsers = parser.add_subparsers(title="Commands", help='Choose an action to take')
    submit_parser = subparsers.add_parser('submit', help='Submit job(s)',
                                          description="Outputs a file <batch_id>.batch.txt containing job "
                                                      "information. Use this file to retrieve job status/results by "
                                                      "running 'api.py get -f <batch_id>.batch.txt'")


    def parse_submit(p_args):
        """
        Handle argparse submit subcommand
        """
        if p_args.file:
            res = submit_jobs_file(p_args.file, p_args.email)
        elif p_args.directory:
            res = submit_jobs_directory(p_args.directory, p_args.email)
        else:
            res = ''

        print(res)


    submit_parser.set_defaults(func=parse_submit)
    submit_parser.add_argument('-e', '--email',
                               help='Send updates to this email when job is submitted/started/completed (will not be '
                                    'saved)')
    group = submit_parser.add_argument_group(title='required arguments',
                                             description='Choose one method to select FASTA sequences to submit')
    mx_group = group.add_mutually_exclusive_group(required=True)
    mx_group.add_argument('-f', '--file',
                          help='FASTA file(s) with labelled sequences',
                          type=file_path)
    mx_group.add_argument('-d', '--directory',
                          help='Directory containing FASTA files with labelled sequences',
                          type=dir_path)

    get_parser = subparsers.add_parser('get', help='Get job(s)')


    def parse_get(p_args):
        """
        Handle argparse get subcommand
        """
        if p_args.job:
            res = get_job(p_args.job, p_args.slim)
        elif p_args.batch:
            res = get_batch(p_args.batch, p_args.slim)
        elif p_args.file:
            res = get_batch_file(p_args.file, p_args.slim)
        else:
            print("Error parsing arguments")
            return

        if len(res) > 0:
            complete = len([r for r in res if r['complete']])
            failed = len([r for r in res if r['failed']])
            pending = len(res) - complete
            if args.type == 'tab':
                for r in res:
                    if r['complete'] and not r['failed']:
                        # Print tab delimited result CSV for each successfully complete job
                        print('{label}\tOX = {taxaId}\tJobId: {jobId}\tSubmitted: {submittedDate}\tStarted: {'
                              'startedDate}\tExecutionTime: {executionTime}s '
                              .format(**r, taxaId=r['result']['taxa']['id']), file=p_args.outfile)

                        if r['result']['alleleOrder']:
                            allele_order = "\t".join(r['result']['alleleOrder'])
                        else:
                            allele_order = "A\tR\tN\tD\tC\tQ\tE\tG\tH\tI\tL\tK\tM\tF\tP\tS\tT\tW\tY\tV"

                        if r['result']['bases']:
                            print("Pos\tRef\tDepth\tConservation\t" + allele_order,
                                  file=p_args.outfile)

                            for pos, b in enumerate(r['result']['bases']):
                                print("{0:5.0f}\t{1:s}\t{2:6.0f}\t{3:1.6f}\t"
                                      .format(pos+1, b['reference'], b['depth'], b['conservation'])
                                      + "\t".join(["{0:1.6f}".format(s) for s in b['list']]), file=p_args.outfile)

                        print("", file=p_args.outfile)
            else:
                print(json.dumps(res, indent=4), file=p_args.outfile)

            print("Complete: {}, Failed: {}, Pending: {}".format(complete, failed, pending))
        else:
            print('No jobs found')


    get_parser.set_defaults(func=parse_get)
    get_parser.add_argument('-o', '--outfile', nargs='?',
                            help='Write results to outfile instead of stdout, defaults to stdout',
                            type=argparse.FileType('w'), default=sys.stdout)
    get_parser.add_argument('-s', '--slim', action='store_false',
                            help='Retrieve a slim version of the job(s) without results')
    get_parser.add_argument('-t', '--type', choices=['tab', 'json'], default='tab',
                            help='Data type to return, either tabular or json. Default: tab')

    group = get_parser.add_argument_group(title='required arguments',
                                          description='Choose one method to select jobs/batches to retrieve '
                                                      'status/results')
    mx_group = group.add_mutually_exclusive_group(required=True)
    mx_group.add_argument('-f', '--file',
                          type=file_path,
                          help='Retrieve status/results for job submission batch file')
    mx_group.add_argument('-j', '--job',
                          help='Job ID to retrieve')
    mx_group.add_argument('-b', '--batch',
                          help='Batch ID to retrieve')

    delete_parser = subparsers.add_parser('delete', help='Delete job(s)')


    def parse_delete(p_args):
        """
        Handle argparse delete subcommand
        """
        if p_args.job:
            res = delete_job(p_args.job)
        elif p_args.batch:
            res = delete_batch(p_args.batch)
        elif p_args.file:
            res = delete_batch_file(p_args.file)
        else:
            print("Error parsing arguments")
            return

        if res:
            print(res)


    delete_parser.set_defaults(func=parse_delete)
    group = delete_parser.add_argument_group(title='required arguments',
                                             description='Choose one method to select jobs/batches to delete')
    mx_group = group.add_mutually_exclusive_group(required=True)
    mx_group.add_argument('-f', '--file',
                          type=file_path,
                          help='Job submission batch file for which to delete jobs')
    mx_group.add_argument('-j', '--job',
                          help='Job ID to delete')
    mx_group.add_argument('-b', '--batch',
                          help='Batch ID to delete')

    parser.set_defaults(func=lambda x: parser.print_help())
    args = parser.parse_args()

    args.func(args)
