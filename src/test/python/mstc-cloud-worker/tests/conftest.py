import pytest

@pytest.fixture(scope="session")
def client(config):
    from mstc_cloud_worker import Client
    rabbit_cfg = config['spring']['rabbitmq']
    return Client(rabbit_cfg['host'], rabbit_cfg['exchange'], rabbit_cfg['queue']['work'])
    
    
@pytest.fixture(scope="session")
def config():
    from pathlib import Path    
    import os
    import yaml
    
    p = Path(os.getcwd())
    src_dir = p.parent.parent    
    f = os.path.join(os.path.dirname(src_dir), "test", "resources", "application.yaml")
    with open(f, 'r') as stream:
        data_loaded = yaml.safe_load(stream)
    return data_loaded


@pytest.fixture(scope="session")
def data(config):
    from mstc_cloud_worker import Data
    minio_cfg = config['minio']
    return Data(minio_cfg['host'], minio_cfg['port'], minio_cfg['user'], minio_cfg['password'])

        
@pytest.fixture
def inputs():
    from pathlib import Path    
    import os
    
    p = Path(os.getcwd())
    src_dir = p.parent.parent    
    data_dir = os.path.join(os.path.dirname(src_dir), "test", "data")
    files = []
    for file in os.listdir(data_dir):
        files.append(file)        
    return files


@pytest.fixture(scope="session")
def cleanup(request, config, data):
    def after():
        if request.session.testsfailed == 0:
            for obj in data.client.list_objects(config['minio']['bucket']):
                name = obj.object_name.encode('utf-8')
                print(obj.bucket_name, name, obj.last_modified,
                      obj.etag, obj.size, obj.content_type)
                data.client.remove_object(config['minio']['bucket'], name)
            print("Clean run, removed data")

    request.addfinalizer(after)