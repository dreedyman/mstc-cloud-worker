from asyncio import subprocess
import pytest

@pytest.fixture(scope="session")
def client():
    from mstc_cloud_worker import Client
    exchange = 'mstc.exchange'
    work_queue = 'mstc.queue.work'
    return Client('localhost', exchange, work_queue)

@pytest.fixture(scope="session")
def data():
    from mstc_cloud_worker import Data
    return Data("localhost", 9000, "minioadmin", "minioadmin")

        
@pytest.fixture
def inputs():
    from pathlib import Path    
    import os
    
    p = Path(os.getcwd())
    src_dir = p.parent.parent    
    data_dir = os.path.join(os.path.dirname(src_dir), "test", "data")
    files = []
    for file in os.listdir(data_dir):
        files.append(data_dir + "/" + file)        
    return files


@pytest.fixture(scope="function")
def astros():
    import os
    from pathlib import Path
    import subprocess
    
    path = Path(os.path.dirname(__file__))
    images_cmd = ["docker", "images"]
    images = subprocess.Popen(images_cmd, cwd=path, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    images_out = images.stdout.read().decode("utf-8")
    have_astros = False
    for line in images_out.split("\n"):
        if "astros-eap-12.5" in line and "0.3.0" in line:
            have_astros = True
            break   
    if not have_astros:         
        pytest.skip("Need astros-eap version 0.3.0")
    else:    
        yield
    
@pytest.fixture(scope="function")
def cleanup(request, data):
    def after():
        if request.session.testsfailed == 0:
            bucket = request.node.resourceId
            print("bucket: " + bucket)
            data.delete(bucket)            
            print("Clean run, removed data")
            
    request.addfinalizer(after)
    