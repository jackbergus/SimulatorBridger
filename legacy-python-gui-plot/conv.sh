#!/bin/sh
/usr/bin/ffmpeg -i  $1                          \
-f avi -vcodec mpeg4  -vtag XVID                \
-b 800k -qmin 2 -qmax 4                         \
-acodec libmp3lame -ab 64k -ac 2                \
-threads 2                                      \
$1.avi
