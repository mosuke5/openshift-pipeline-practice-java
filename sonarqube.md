# Sonarqube

## Sonarqubeのインストール（管理者タスク）
`app-devops` プロジェクト内にSonarqubeが起動させます。 
SonarqubeのデフォルトID/PWは `admin/admin` です。ログインしてパスワードを変えておきましょう。

```
oc create sa postgresql
oc adm policy add-scc-to-user anyuid -z postgresql
$ helm repo add sonarqube https://SonarSource.github.io/helm-chart-sonarqube
$ helm repo update
$ helm upgrade --install -f etc/sonarqube.yaml -n app-devops sonarqube sonarqube/sonarqube
```

`common-sonar-key`という名前でSonarqubeにアクセスできる共通鍵をクレデンシャルストアに保管しておきます。

## Sonarqubeにアクセス
以下にアクセスしログインできるか確認しましょう。
ログインIDは、管理者より連絡します。

## プロジェクト作成
ログインができたら、Sonarqubeにプロジェクトを作りましょう。
プロジェクト名は他の人とかぶらないように `userxx-app` としておきましょう。

![](/images/sonarqube-create-project.png)

## パイプラインの修正
Sonarqubeを利用するようにパイプラインを修正します。  
`environment`内の`sonar_name`および`sonar_key`のコメントアウトを外して、`sonar_name`を、前項番で作成したプロジェクト名に変更しましょう。

さらに `stage('Analysis code')`のコメントアウトも外しましょう。

```groovy
//Jenkinsfile
//...
  environment {
    deploy_branch  = "origin/main"
    deploy_project = "userxx-development"
    app_name       = 'pipeline-practice-java'
    app_image      = "image-registry.openshift-image-registry.svc:5000/${deploy_project}/${app_name}"
    sonar_name     = 'userxx-app'
    sonar_key      = credentials('common-sonar-key')
  }
  //...

    stage('Analysis code') {
      steps {
        echo "Exec sonar scanner"
        sh 'mvn verify sonar:sonar -DskipTests=true \
              -Dsonar.projectKey=$sonar_name \
              -Dsonar.host.url=http://sonarqube-sonarqube.app-devops:9000 \
              -Dsonar.login=$sonar_key \
              -Dsonar.qualitygate.wait=true'
      }
    }
```

## パイプラインの再実行とデータ閲覧
パイプラインの修正が終わったら、もう一度パイプラインを実行してみましょう。
パイプラインがうまく動作すれば、Sonarqubeのプロジェクトにデータがアップロードされます。

![](/images/sonarqube-metrics.png)

Sonarqubeでは、さまざまなメトリクスを取得しています。 特にMeasuresの中身を確認しておくと面白いでしょう。

- Code Smells
- Coverage
    - 単体テストのカバレッジ。
    - コードの行ごとのカバー箇所
- Complexity
    - コードの複雑度です。このアプリケーションではコード量が少ないので参考になりづらいですが、今後皆さんのアプリケーションを解析するときは役に立つでしょう。
- Duplications
    - コードの重複度

## Quality Gateの設定変更
次にQuality Gateの設定をより厳しくしてみます。  
このサンプルアプリケーションは、不幸なことにコードカバレッジが50%以下です。
このスクラムチームでは、コードカバレッジが70%以上であることをDoneの定義にしているとします。

Sonarqubeのプロジェクト内から"Project Settings" -> "Quality Gate"を選択し、"Always user a specific Quality Gate"で管理者が作成したプロファイルを選択しましょう。

その上で、もう一度パイプラインを実行して何が起こるか観察してみましょう。

次のように、Quality GateのPassに失敗したのではないかと思います。

```
[INFO] ------------- Check Quality Gate status
[INFO] Waiting for the analysis report to be processed (max 300s)
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  41.648 s
[INFO] Finished at: 2023-01-24T13:05:30Z
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184:sonar (default-cli) on project freelancer-service: QUALITY GATE STATUS: FAILED - View details on http://sonarqube-sonarqube.app-devops:9000/dashboard?id=userxx-app -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoExecutionException
```
