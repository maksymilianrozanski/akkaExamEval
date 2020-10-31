rem requests adding new questions set
curl --user admin:pass -X POST "localhost:8080/repo/add" -d "@json/setWithImages.json" -v -H "Content-Type: application/json"
