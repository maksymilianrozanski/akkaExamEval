rem requests exam evaluation at student/evaluate endpoint
curl -X POST  "localhost:8080/student/evaluate" -d "@json/examToEvalCompact.json" -v -H "Content-Type: application/json"
