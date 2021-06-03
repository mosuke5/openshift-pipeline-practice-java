# Getting started
## project作成
OpenShiftのprojectを2つ作成する。
`userxx-development`がサンプルのアプリケーションが動作するprojectで、`app-devops`はJenkinsが動作するprojectである。
アプリケーションが動作するために必要なリソースを確保しておくため、アプリケーションが動作するprojectと開発に必要なツールは分離しておくことをおすすめする。

```
$ oc new-project userxx-development
$ oc new-project app-devops
```

## Jenkins起動
Jenkinsを起動する。Jenkinsは動作が重いため、必要に応じて`resource`を調整する。
また、Jenkinsはのちに`userxx-development`のリソースを操作するために権限を付与しておく。
カスタムJenkinsを作成し、管理したい場合は[こちらのレポジトリ](https://github.com/mosuke5/openshift-custom-jenkins)を参考にしてJenkinsの起動を行う。

```
$ oc new-app jenkins-persistent --param ENABLE_OAUTH=true --param MEMORY_LIMIT=2Gi --param VOLUME_CAPACITY=10Gi --param DISABLE_ADMINISTRATIVE_MONITORS=true
...
--> Success
    Access your application via route 'jenkins-app-devops.apps.na311.openshift.opentlc.com'
    Run 'oc status' to view your app.

$ oc policy add-role-to-user edit system:serviceaccount:app-devops:jenkins -n userxx-development
clusterrole.rbac.authorization.k8s.io/edit added: "system:serviceaccount:app-devops:jenkins"
```

なお、上記の設定でもJenkinsの動作が重い場合は、DeploymentConfigを編集して、リソースの割り当てを変更できる。
```
$ oc edit deploymentconfigs.apps.openshift.io jenkins
```
```
(設定例)
...
        resources:
          limits:
            cpu: "2"
            memory: 4Gi
          requests:
            cpu: "2"
            memory: 4Gi
...
```

## Jenkins agent imageの作成
Jenkinsパイプラインを実際に動作させるJenkins agentのイメージを作成する。
`jenkins-agent-maven`をベースとしながら、テストに必要なpostgresqlのクライアントをインストールしたJenkins agentを作成する。
作成方法は、Dockerfileからビルドする。

```
$ cat etc/Dockerfile_jenkins_agent
$ oc process -f openshift/custom-jenkins-agent.yaml | oc apply -n app-devops -f -
buildconfig.build.openshift.io/custom-jenkins-agent-maven created
imagestream.image.openshift.io/custom-jenkins-agent-maven created

$ oc start-build custom-jenkins-agent-maven -n app-devops
```

[Jenkinsfile](./Jenkinsfile)のagent設定にて、イメージ名に`custom-jenkins-agent-maven`を指定されていることを確認する。

```
$ cat Jenkinsfile
...
  agent {
    kubernetes {
      cloud 'openshift'
      yaml """\
        apiVersion: v1
        kind: Pod
        spec:
          serviceAccountName: jenkins
          containers:
            - name: jnlp
              image: image-registry.openshift-image-registry.svc:5000/app-devops/custom-jenkins-agent-maven
              args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
...
```

## Jenkinsの設定
おそらく、上のJenkins agent imageを作っている間にJenkinsが起動したはずだ。
Jenkins側の設定をいくつか行う。

### Jenkinsへのアクセス
Jenkinsをテンプレートから起動しているため、routeが作成されている。
このURLからJenkinsへアクセス可能。ログインはOpenShiftのUserと連携しているのでユーザ管理が不要。

```
$ oc get route
NAME      HOST/PORT                      PATH   SERVICES   PORT    TERMINATION     WILDCARD
jenkins   jenkins-app-devops.apps.xxxx          jenkins    <all>   edge/Redirect   None
```

### プラグイン
Webhook利用するため新規にgeneric webhookプラグインをインストールする。
また、古いOpenShift（4.1など）を利用している場合、いくつかのデフォルトプラグインが古く動作しないことがあるので、アップデートも必要に応じて行う。
また、サンプルのためGUIからプラグインをインストールするが、Jenkins設定をS2Iでビルドすることが可能。実運用ではS2Iでのビルドを検討する。

- インストール
    - Generic Webhook Trigger
- アップデート（必要に応じて）
    - kubernetes
    - Pipeline: declarative
    - Git

### プロジェクト
プラグインのアップデートとインストールが終わったら、アプリケーションのパイプラインを実行するためにJenkins Itemを作成する。

1. "New Item"を選択
1. Itemの種類は"Pipeline"を選択し、任意の名前をつける
1. "Build Triggers"項目で"Generic Webhook Trigger"にチェックを付け下記を設定する（手動で実行する場合は設定しなくてもいい）
    - token: 任意の文字列
1. "Pipeline"の項目で実行するパイプライン定義を設定する
    - "pipeline script from SCM"を選択
    - レポジトリとブランチを入力する

### GitレポジトリへWebhook設定
お使いのGitレポジトリへWebhookの設定を行う。
Generic Webhook Triggerの場合下記のURLでトリガーできる。

```
https://jenkins-app-devops.xxxxx.com/generic-webhook-trigger/invoke?token=<your-token>
```

## パイプライン実行
ここまでできたらパイプラインを実行すれば、アプリケーションがデプロイされる。
アプリケーションの変更にも自動でデプロイできる状態だ。
レポジトリに任意の変更を加えてみよう。

![jenkins-stage-view](/images/jenkins-stage-view.png)

## 動作確認
初回は、データベースがないため、アプリケーションの起動に一度失敗するが、
データベースが起動後にアプリケーションが自動的に起動するはずだ。

最後は起動したアプリケーションにブラウザから接続して確認してみよう。
URLは`https://xxxxxxxxxxxx/health` or `https://xxxxxxxxxxxx/freelancers`

## カスタマイズ
以下は、運用環境に合わせてカスタマイズできる項目の例。

### プライベートのGitレポジトリを扱いたい
プライベートのGitレポジトリで行う場合には、JenkinsとOpenShiftの両方にプライベートレポジトリにアクセス可能なキーを登録する必要がある。JenkinsではSCM内のJenkinsfile取得やチェックインで利用し、OpenShiftではBuildConfigでビルドする際のソースコードとして必要。

### BuildConfigでのプライベートレポジトリへのアクセス
OpenShift内にシークレットとしてキーを登録する。`builder`から利用できるようにする。

```
$ oc create secret generic git-repo-key -n userxx-development --from-file=ssh-privatekey=/path/tp/id_rsa
$ oc secrets link builder git-repo-key
```

BuildConfig内で上記のキーを利用するように指定する。
```yaml
# BuildConfigでの指定
  source:
    type: Git
    git:
      uri: 'git@github.com:xxxx/xxxx.git'
      ref: master
    sourceSecret:
      name: git-repo-key
```

### Jenkins内でのプライベートレポジトリへのアクセス
Credentialを利用する。  
https://jenkins.io/doc/book/using/using-credentials/

### Jenkinsfileを書きたい
Jenkins piplelineの記法は、DeclarativeとScriptの2つがある。
どちらをベースに書くか意識することが必要。現在であればDeclarative記法をベースにすることが望ましい。

Declarativeのシンタックスはこちらから確認できる。  
https://jenkins.io/doc/book/pipeline/syntax/

### BuildConfigのJenkinspipelineとどう使い分けたら良いの？
BuildConfigのJenkinspipelineはdeprecatedなので、気にしなくて良い。

### Jenkins パイプラインのテストで外部コンポーネントを使いたい
パイプライン上のテストでDBなど外部リソースを使いたい場合は、サイドカーとしてDBのコンテナを立ち上げるとよい。
JenkinsのKubernetes Pod templateの設定で複数のコンテナを指定できる。例えばそこで、メインのjnlpと別にmysqlコンテナを起動しておけばいい。

実際に下記のように、Jenkins agent podはコンテナ２つで起動する。

```
test-pipeline-1-nffqh-xkxl0-ftjrd   2/2       Running   0         35s
```

### Jenkins agentのコンテナイメージをカスタマイズしたい
デフォルトではmavenとnodejsの環境のみJenkins agentのイメージが用意されている。
独自のアプリケーションが動作する環境を作るためにはJenkins agentのコンテナイメージをカスタマイズする必要がある。
以下のレポジトリのJenkins agentの例をベースにするとカスタマイズすると楽。  
https://github.com/redhat-cop/containers-quickstarts/

以下に、jenkins agentのベースイメージもあるので、こちらを元にカスタマイズすることもできる。  
https://quay.io/repository/openshift/origin-jenkins-agent-base?tab=tags
