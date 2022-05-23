import pytest
from pathlib import Path
import os


def test_send(client, data, inputs, prefix, cleanup, request):
    request.node.resourceId = "test.inputs"
    print("Upload...")
    items = data.upload("test.inputs", inputs, prefix)
    assert items is not None
    assert len(items) == 2
    job_request = {"image": "mstc/python-test",
                   "jobName": "test-job",
                   "inputBucket" : "test.inputs",
                   "prefix": prefix,
                   #"outputBucket" : "test.outputs"
                   }    
    print("Send...")
    result = client.send(job_request)
    content = result["result"]
    print()
    parts = content.split(" ")
    job_unique_name = prefix + "-" + parts[1]     
    
    path = Path(os.path.dirname(__file__))
    files_dir = os.path.join(path, "scratch")
    print("Download...")
    downloaded = data.download("test.inputs", prefix, files_dir)
    # assert len(downloaded) == 3
    assert succeeded(job_unique_name, files_dir, "SUCCESS")

def test_astros(client, data, inputs, astros, prefix):
    items = data.upload("astros.inputs", inputs, prefix)
    assert items is not None
    assert len(items) == 2
    job_request = {"image": "mstc/astros-eap-12.5:0.4.0",
                   "jobName": "astros-job",
                   "inputBucket" : "astros.inputs",
                   "outputBucket" : "astros.outputs",
                   "prefix": prefix,
                   }    
    result = client.send(job_request)
    content = result["result"]
    print()
    parts = content.split(" ")
    job_unique_name = prefix  + "-" + parts[1]
    
    path = Path(os.path.dirname(__file__))
    files_dir = os.path.join(path, "scratch")
        
    downloaded = data.download("astros.outputs", prefix, files_dir)
    assert len(downloaded) == 2
    assert succeeded(job_unique_name, files_dir, "Finished processing")
    
    
def succeeded(name, path, expected):
    from os import listdir
    from os.path import isfile, join
    files = [f for f in listdir(path) if isfile(join(path, f))]
    matched = False
    for f in files:
        if f.startswith(name):
            file = os.path.join(path, f)
            with open(file) as result:
                if expected in result.read():
                    matched = True
    return matched
         
    

