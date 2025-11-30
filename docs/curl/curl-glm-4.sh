curl -X POST \
        -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsInNpZ25fdHlwZSI6IlNJR04ifQ.eyJhcGlfa2V5IjoiMDI0YWQxMjI5MTg2NDFkYjk5NmExYWI4YWU5YjEyZTgiLCJleHAiOjE3NjQ0MjM2MDE0MzAsInRpbWVzdGFtcCI6MTc2NDQyMTgwMTQ0Nn0.-nZHg2kgsoyt5jn2tEJGIuRXr-YiW1UXigqyY4Iu2I0" \
        -H "Content-Type: application/json" \
        -H "User-Agent: Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)" \
        -d '{
          "model":"glm-4.5-flash",
          "stream": "true",
          "messages": [
              {
                  "role": "user",
                  "content": "1+1"
              }
          ]
        }' \
  https://open.bigmodel.cn/api/paas/v4/chat/completions