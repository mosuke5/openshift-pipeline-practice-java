# Sonarqube

## Sonarqubeのインストール
`app-devops` プロジェクト内にSonarqubeが起動させます。 
SonarqubeのデフォルトID/PWは `admin/admin` です。ログインしてパスワードを変えておきましょう。

```
oc create sa postgresql
oc adm policy add-scc-to-user anyuid -z postgresql
$ helm repo add sonarqube https://SonarSource.github.io/helm-chart-sonarqube
$ helm repo update
$ helm upgrade --install -f etc/sonarqube.yaml -n app-devops sonarqube sonarqube/sonarqube
```

## プロジェクト作成



```
mvn test jacoco:report 
cat target/site/jacoco/jacoco.xml
```

```
mvn verify sonar:sonar -DskipTests=true \
  -Dsonar.projectKey=userxx-app \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=xxxxx \
  -Dsonar.qualitygate.wait=true
```