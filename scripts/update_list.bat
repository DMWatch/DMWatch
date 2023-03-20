cur_datetime=`date -u +%FT%TZ`

git config --local user.name DMW-Updater
git config --local user.email "redzguilt@gmail.com"
git add ../data/mixedlist.json
git commit -m "[${cur_datetime}] watchlist update"
git push remote main

echo [INFO] Watchlist successfully updated