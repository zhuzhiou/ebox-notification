# ebox-notification单机部署

## 构建Docker镜像

使用了maven的profie，默认激活了本地开发环境，如果打包部署到生产环境，需要指定profile，同时需要设置git用户密码等环境变量。

```
mvn clean package dockerfile:build -Pprod
```

## 使用阿里云容器镜像服务

```
docker login --username=zhuzhiou@qq.com registry.cn-shenzhen.aliyuncs.com
```

## 将镜像推送到Registry

```
docker push registry.cn-shenzhen.aliyuncs.com/ebox/ebox-notification:latest
```

## 拉取镜像

```
docker pull registry.cn-shenzhen.aliyuncs.com/ebox/ebox-notification:latest
```

## 运行应用程序

```
docker run -it --rm --name ebox-notification --dns 172.16.159.38 registry.cn-shenzhen.aliyuncs.com/ebox/ebox-notification:latest
```

## 知识点

