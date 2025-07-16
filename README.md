仿真数据生成器

建议演示情况（10-50条数据）用ai引擎生成高度拟真数据，测试性能情况（100万以上数据）使用随机引擎生成。

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

----20250715---
真是无语了，deepseek api是狗屎吗，把提示压缩到一句话还能信口回答胡说八道，简直一坨。准备换个AI引擎。
<img width="1385" height="55" alt="image" src="https://github.com/user-attachments/assets/69c9ca17-1b17-4a50-a2db-99b05b5d6e77" />
<img width="1376" height="781" alt="image" src="https://github.com/user-attachments/assets/3fe9b9da-2c4b-41cc-a5d1-b9008ba56c25" />

----20250716---
用豆包现在体验好多了，速度大幅度提升，一致性和准确度也可靠的多。

<img width="1277" height="311" alt="image" src="https://github.com/user-attachments/assets/c117b145-bff5-4109-b01d-3840c4d3a779" />
