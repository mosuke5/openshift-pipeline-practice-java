# Getting started
## project作成
OpenShiftのprojectを2つつくる。
`app-development`がサンプルのアプリケーションが動作するprojectで、`app-devops`はJenkinsが動作するprojectである。
アプリケーションが動作するために必要なリソースを確保しておくため、アプリケーションが動作するprojectと開発に必要なツールは分離しておくことをおすすめする。

```
$ oc new-project app-development
$ oc new-project app-devops
```

## Jenkins起動
Jenkinsを起動する。Jenkinsは動作が重いため、必要に応じて`resource`を調整してください。
また、Jenkinsはのちに`app-development`のリソースを操作するために権限を付与しておく。

```
$ oc new-app jenkins-persistent --param ENABLE_OAUTH=true --param MEMORY_LIMIT=2Gi --param VOLUME_CAPACITY=10Gi --param DISABLE_ADMINISTRATIVE_MONITORS=true
...
--> Success
    Access your application via route 'jenkins-app-devops.apps.na311.openshift.opentlc.com'
    Run 'oc status' to view your app.

$ oc policy add-role-to-user edit system:serviceaccount:app-devops:jenkins -n app-development
clusterrole.rbac.authorization.k8s.io/edit added: "system:serviceaccount:app-devops:jenkins"
```

## Slave imageの作成
Jenkinsパイプラインを実際に動作させるJenkins slaveのイメージを作成する。
`jenkins-agent-maven`をベースとしながら、テストに必要なpostgresqlのクライアントをインストールしたJenkins Slaveを作成する。
作成方法は、Dockerfileからビルドする。

```
$ cat etc/Dockerfile_jenkins_agent
$ oc process -f openshift/custom-jenkins-agent.yaml | oc apply -n app-devops -f -
buildconfig.build.openshift.io/custom-jenkins-agent-maven created
imagestream.image.openshift.io/custom-jenkins-agent-maven created

$ oc start-build custom-jenkins-agent-maven -n app-devops
```

## Jenkinsの設定
おそらく、上のSlave Imageを作っている間にJenkinsが起動したはずだ。
Jenkins側の設定をいくつか行う。

### プラグイン
本サンプルで利用する、Jekinsfileの記述ではデフォルのプラグインでは古く動作しないためアップデートを行う。
また、Webhookを簡単に利用できるようにするために新規にプラグインをインストールする。

- アップデート
    - kubernetes
    - Pipeline: declarative
- インストール
    - generic webhook

### プロジェクト
プラグインのアップデートとインストールが終わったら、アプリケーションのパイプラインを実行するためにJenkins Itemを作成する。

1. "New Item"を選択
1. Itemの種類は"Pipeline"を選択し、任意の名前をつける
1. "Build Triggers"項目で"Generic Webhook Trigger"にチェックを付け下記を設定する
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

# カスタマイズ
## プライベートのGitレポジトリを扱いたい
プライベートのGitレポジトリで行う場合には、JenkinsとOpenShiftの両方にプライベートレポジトリにアクセス可能なキーを登録する必要がある。JenkinsではSCM内のJenkinsfile取得やチェックインで利用し、OpenShiftではBuildConfigでビルドする際のソースコードとして必要。

### BuildConfigでのプライベートレポジトリへのアクセス
OpenShift内にシークレットとしてキーを登録する。`builder`から利用できるようにする。

```
$ oc create secret generic git-repo-key -n app-development --from-file=ssh-privatekey=/path/tp/id_rsa
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

## Jenkinsfileを書きたい
Jenkins piplelineの記法は、DeclarativeとScriptの2つがある。
どちらをベースに書くか意識することが必要。現在であればDeclarative記法をベースにすることが望ましい。

Declarativeのシンタックスはこちらから確認できる。  
https://jenkins.io/doc/book/pipeline/syntax/

### BuildConfigのJenkinspipelineとどう使い分けたら良いの？
BuildConfigのJenkinspipelineはdeprecatedなので、気にしなくて良い。

## Jenkins パイプラインのテストで外部コンポーネントを使いたい
パイプライン上のテストでDBなど外部リソースを使いたい場合は、サイドカーとしてDBのコンテナを立ち上げるとよい。
JenkinsのKubernetes Pod templateの設定で複数のコンテナを指定できる。例えばそこで、メインのjnlpと別にmysqlコンテナを起動しておけばいい。

実際に下記のように、slave podはコンテナ２つで起動する。

```
jenkins-slave-ruby-rmmhh   2/2       Running   0         35s
```

## Jenkins Slaveのコンテナイメージをカスタマイズしたい
デフォルトではmavenとnodejsの環境のみJenkins Slaveのイメージが用意されている。
独自のアプリケーションが動作する環境を作るためにはJenkins Slaveのコンテナイメージをカスタマイズする必要がある。
以下のレポジトリのJenkins Slaveの例をベースにするとカスタマイズすると楽。  
https://github.com/redhat-cop/containers-quickstarts/

以下に、jenkins slaveのベースイメージもあるので、こちらを元にカスタマイズすることもできる。  
https://quay.io/repository/openshift/origin-jenkins-agent-base?tab=tags
