```
fetch("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=MSFT&apikey=demo").then(function(response) {
  return response.json();
}).then(function(json) {
  console.log(json["Time Series (Daily)"]["2018-02-02"]["5. volume"])
})
```


```
https://github.com/semberal/semberal.github.io/blob/master/scala-future-timeout-patterns.md
```
