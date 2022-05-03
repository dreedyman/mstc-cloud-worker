import pytest
from pathlib import Path
import os
#from mstc_cloud_worker import Client
#from mstc_cloud_worker import Data


def test_send(client, data, inputs, cleanup, request):
    request.node.resourceId = "test.inputs"
    print("Upload...")
    items = data.upload("test.inputs", inputs)
    assert items is not None
    assert len(items) == 2
    job_request = {"image": "mstc/python-test",
                   "jobName": "test-job",
                   "inputBucket" : "test.inputs",
                   #"outputBucket" : "test.outputs"
                   }    
    print("Send...")
    result = client.send(job_request)
    content = result["result"]
    for line in content.split("\n"):
        print(line)
    
    path = Path(os.path.dirname(__file__))
    files_dir = os.path.join(path, "scratch")
    print("Download...")
    downloaded = data.download("test.inputs", files_dir)
    assert len(downloaded) == 3    

"""    
def test_astros(client, data, inputs):
    items = data.upload("astros.inputs", inputs)
    assert items is not None
    assert len(items) == 2
    job_request = {"image": "mstc/astros-eap-12.5:0.3.0",
                   "jobName": "astros-job",
                   "inputBucket" : "astros.inputs",
                   "outputBucket" : "astros.outputs"
                   }    
    result = client.send(job_request)
    content = result["result"]
    for line in content.split("\n"):
        print(line)
    
    path = Path(os.path.dirname(__file__))
    files_dir = os.path.join(path, "scratch")
        
    downloaded = data.download("astros.outputs", files_dir)
    assert len(downloaded) == 1
    
"""

