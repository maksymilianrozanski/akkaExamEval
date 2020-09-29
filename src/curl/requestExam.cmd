rem requests new exam at student/start2 endpoint
curl -X POST  "localhost:8080/student/start2" -d "@json/studentsRequest.json" -v -H "Content-Type: application/json"
