def download(endpoint, bucket):
    print("Download all files from "+ endpoint+"/"+ bucket +" ...")
    
            
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
    if "OUTPUT_BUCKET" in os.environ:
        output_bucket = os.environ['OUTPUT_BUCKET']
    else:
        output_bucket = input_bucket
                    
    logging.debug("endpoint:  " + endpoint)
    logging.debug("input bucket:    " + input_bucket)
    logging.debug("output bucket:   " + output_bucket)
    logging.debug("Pretend to do work...")
    for k, v in sorted(os.environ.items()):
        print(k + ':', v)
        
    download(endpoint, input_bucket)    
    time.sleep(5)
    print("host: " + host)
    print("port: " + port)
    print("Wrote 5 items to " + endpoint +"/" + output_bucket)
    
        