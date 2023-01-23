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
