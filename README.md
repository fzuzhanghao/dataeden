仿真数据生成器


基于gradle+spring boot3构建，直接找到主类DataEdenApplication运行即可，需要在application.yml使用你自己的api_key
前端cdn方式使用vue，挂在static下
http://localhost:18080/datasource.html

很简单的功能
输入连接信息-选择表-选择数据引擎-生成数据

![image](https://github.com/user-attachments/assets/233e4a9b-cd8a-45ca-95f2-aa2495467ebb)
![image](https://github.com/user-attachments/assets/89f3c81f-984a-4670-add4-5552e62cf1fa)
![image](https://github.com/user-attachments/assets/b388f2fa-8edf-4cda-8a3b-e182b32238cc)
![image](https://github.com/user-attachments/assets/620c04f6-e9f9-4cd3-87ec-3b7acd45c466)

目前存在部分问题，deepseek api还是有点蠢，多请求几次就会生成些乱七八糟的数据导致格式无法匹配。
