import os
from tempfile import TemporaryFile
from urllib.parse import urlparse
from os.path import exists
import posixpath
try:
    from urlparse import urlsplit
    from urllib import unquote
except ImportError: # Python 3
    from urllib.parse import urlsplit, unquote

import requests

def url2filename(url):
    """Return basename corresponding to url.
    >>> print(url2filename('http://example.com/path/to/file%C3%80?opt=1'))
    fileÀ
    >>> print(url2filename('http://example.com/slash%2fname')) # '/' in name
    Traceback (most recent call last):
    ...
    ValueError
    """
    urlpath = urlsplit(url).path
    basename = posixpath.basename(unquote(urlpath))
    if (os.path.basename(basename) != basename or
        unquote(posixpath.basename(urlpath)) != basename):
        raise ValueError  # reject '%2f' or 'dir%5Cbasename.ext' on Windows
    return basename

def is_local(url):
    url_parsed = urlparse(url)
    if url_parsed.scheme in ('file', ''):  # Possibly a local file
        return exists(url_parsed.path)
    return False

def download(url, file):
    r = None
    f = None
    if isinstance(file, str):
        if not os.path.exists(file):
            print(f"downloading {url}")
            r = requests.get(url, verify=False, stream=True)
            r.raw.decode_content = True
            f = open(file, 'wb')
    else:
        print(f"downloading {url}")
        r = requests.get(url, verify=False, stream=True)
        r.raw.decode_content = True
        f = file
    for chunk in r.iter_content(chunk_size=1024):
        if chunk:
            f.write(chunk)
    f.close()



class ReadFileContent:
    def __init__(self, path, download_dest=None):
        self.path = path
        self.tmp = None
        self.download_dest = download_dest

    def __enter__(self):
        import tempfile
        if is_local(self.path):
            self.fd = open(self.path, 'r')
        else:
            self.tmp = tempfile.NamedTemporaryFile() if self.download_dest is None else self.download_dest
            with open(self.tmp.name, 'wb') as f:
                download(self.path, f)
            self.fd = open(self.tmp.name, "r")
        return self

    def read(self, n=-1):
        return self.fd.read(n)

    def readlines(self):
        return self.fd.readlines()

    def readline(self):
        return self.fd.readline()

    def getFile(self):
        return self.fd

    def __iter__(self):
        return self

    def __next__(self):
        val = next(self.fd)
        return val

    def __exit__(self, exception_type, exception_value, exception_traceback):
        self.fd.close()
        if self.tmp is not None:
            self.tmp.close()
            self.tmp = None
