# Getting started
## レポジトリのフォーク
Githubでこのレポジトリをフォークし、自分のアカウントにレポジトリをコピーしましょう。

また1箇所変更が必要です。Jenkinsfileの3行目を自分のプロジェクト名に変更しましょう。

```
$ vim Jenkinsfile
def deploy_project = "userX-development"
```

## Jenkinsへのアクセス
- URL: xxxxxxxxxxx
- ID/PW: OpenShiftのアカウントです

## プロジェクト
アプリケーションのパイプラインを実行するためにJenkins Itemを作成する。

1. "New Item"を選択
1. Itemの種類は"Pipeline"を選択し、任意の名前をつける。わかりやすいようにユーザ名を先頭に付けてください。
1. "Build Triggers"項目で"Generic Webhook Trigger"にチェックを付け下記を設定する
    - token: 任意の文字列
1. "Pipeline"の項目で実行するパイプライン定義を設定する
    - "pipeline script from SCM"を選択
    - SCMに"Git"を選択
    - Repository URLにはフォークした自分のレポジトリのURLを入力
    - Branches to buildは`*/20200518`を入力
    - これでSave

## GitレポジトリへWebhook設定
お使いのGitレポジトリへWebhookの設定を行う。
Generic Webhook Triggerの場合下記のURLでトリガーできる。

```
https://<jenkins-url>/generic-webhook-trigger/invoke?token=<your-token>
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

## なにが起きているのか解説
ここまでできたところで、一度なにが行われたのか解説します。

- Jenkinsfile
  - test
  - application build
  - image build
  - deploy
- Kubernetes manifests
- Jenkins agent

## アプリケーションに変更を加える

## パイプラインに処理を追加する

