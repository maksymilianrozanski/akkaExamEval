rem requests exam evaluation at student/evaluate endpoint
curl -X POST  "localhost:8080/student/evaluate" -d "@json/examToEval.json" -v -H "Content-Type: application/json"
