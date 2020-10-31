rem requests adding new questions set
curl --user admin:pass -X POST "localhost:8080/repo/add" -d "@json/questionsSet.json" -v -H "Content-Type: application/json"
