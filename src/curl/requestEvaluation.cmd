rem requests exam evaluation at student/evaluate endpoint
curl -X POST  "localhost:8080/student/evaluate" -d "@json/examToEval.json" -v -H "Content-Type: application/json" -H "Authorization:eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2MDI1MDIxNzQsImlhdCI6MTYwMTg5NzM3NCwiZXhhbUlkIjoiMSJ9.IJl9GJDc6heR8K3hlnZzCg8G_5zOUDwVeIm5VZjGUrw"
