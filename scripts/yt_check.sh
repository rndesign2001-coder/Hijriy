#!/bin/bash
mkdir -p out
for id in UCos52azQNBgW63_9uDJoPDA UCWainKMJPyXikjekccFf3NA; do
  for ref in "" "https://uz.hijriy.app/"; do
    echo "== $id ref=$ref" >> out/log.txt
    if [ -n "$ref" ]; then H=(-H "Referer: $ref"); else H=(); fi
    curl -sL "${H[@]}" -A "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36" \
      "https://www.youtube.com/embed/live_stream?channel=$id" -o page.html
    grep -o '"playabilityStatus":{[^}]*}' page.html | head -2 >> out/log.txt
    grep -o '"videoId":"[^"]*"' page.html | head -2 >> out/log.txt
    grep -o 'errorcode[^,]*' page.html | head -2 >> out/log.txt
  done
  echo "== live page $id" >> out/log.txt
  curl -sL -A "Mozilla/5.0" -H "Accept-Language: en" "https://www.youtube.com/channel/$id/live" -o live.html
  grep -o '"playabilityStatus":{"status":"[^"]*"' live.html | head -1 >> out/log.txt
  grep -o '"playableInEmbed":[a-z]*' live.html | head -1 >> out/log.txt
  grep -o '<link rel="canonical" href="[^"]*"' live.html | head -1 >> out/log.txt
  grep -o '"isLive":[a-z]*\|"isLiveContent":[a-z]*' live.html | head -2 >> out/log.txt
done
