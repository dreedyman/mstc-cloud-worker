def download(endpoint, bucket, prefix):
    print("Download all files from "+ endpoint+"/"+ bucket +" with prefix " + prefix + " ...")
    
            
if __name__ == "__main__":
    import os
    import time
    import logging
    
    logging.basicConfig(format='%(asctime)s %(message)s', level=logging.INFO)    
    logging.info("Started")
    
    host = os.environ['MINIO_SERVICE_HOST']
    port = os.environ['MINIO_SERVICE_PORT']    
    endpoint = "http://" + host + ":" + port
    input_bucket = os.environ['INPUT_BUCKET']
    output_bucket = os.environ['OUTPUT_BUCKET']
    if 'FILE_PREFIX' in os.environ:
        prefix = os.environ['FILE_PREFIX']
    else:
        prefix = ""
                    
    logging.debug("endpoint:  " + endpoint)
    logging.debug("input bucket:    " + input_bucket)
    logging.debug("output bucket:   " + output_bucket)
    logging.debug("Pretend to do work...")
    for k, v in sorted(os.environ.items()):
        print(k + ':', v)
        
    download(endpoint, input_bucket, prefix)    
    time.sleep(5)
    print("host: " + host)
    print("port: " + port)
    print("SUCCESS")
    
        