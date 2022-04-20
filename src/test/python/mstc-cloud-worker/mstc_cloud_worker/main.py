def parse_url(url):
    from urllib.parse import urlparse
    url_parts = urlparse(url)
    endpoint = url_parts.scheme + "://" + url_parts.netloc
    path = url_parts.path
    if path.startswith("/"):
        path = path[1:]
    return endpoint, path
        
        
if __name__ == "__main__":
    import os
    import time
    import logging
    
    logging.basicConfig(format='%(asctime)s %(message)s', level=logging.INFO)    
    logging.info("Started")
    input_bucket_url = os.environ['INPUT_BUCKET_URL']
    input_endpoint, input_bucket = parse_url(input_bucket_url)
    
    if "OUTPUT_BUCKET_URL" in os.environ:
        output_endpoint, output_bucket = parse_url(os.environ['OUTPUT_BUCKET_URL'])
    else:
        output_endpoint = input_endpoint
        output_bucket = input_bucket
                    
    logging.debug("input endpoint:  " + input_endpoint)
    logging.debug("input bucket:    " + input_bucket)
    logging.debug("output endpoint: " + output_endpoint)
    logging.debug("output bucket:   " + output_bucket)
    logging.debug("Pretend to do work...")
    time.sleep(5)
    logging.info("Wrote 5 items to " + output_endpoint +"/" + output_bucket)
    
        