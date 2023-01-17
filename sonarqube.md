# Sonarqube

## Sonarqubeのインストール
`app-devops` プロジェクト内にSonarqubeが起動させます。 
SonarqubeのデフォルトID/PWは `admin/admin` です。ログインしてパスワードを変えておきましょう。

```
$ helm repo add sonarqube https://SonarSource.github.io/helm-chart-sonarqube
$ helm repo update
$ helm upgrade --install -f docs/solutions/sonarqube.yaml -n app-devops sonarqube sonarqube/sonarqube
```