オリジナル
https://github.com/intersystems-community/irisdemo-demo-htap
https://github.com/intersystems-community/irisdemo-demo-htap/blob/master/ICM/README.md (AWS)

日本語でのご紹介資料
https://www.intersystems.com/isc-resources/wp-content/uploads/sites/24/ESG-Technical-Review-InterSystems-IRIS-June-2020_JP.pdf

主な変更点
Tokyo region用に修正
IRIS DB初期サイズを1GB固定に
docker-compoose-enterprise-iris.ymlで標準のIRISイメージを使用(個別ビルドせず)
docker-compoose-enterprise-iris.ymlにcpf mergeを追加(初期パスワード指定のため)
docker-compoose-enterprise-iris.yml使用時のIngestion Thread per workerを1に
ICM環境はCloudFormationを使用して作成

HowToRunInTokyo.txt
デプロイ方法の説明・実行。ベンチ実行。

how_to_debug_in_eclipse/　
  how_to_import_to_eclipse.txt
  gitソースからEclipseプロジェクトを開く方法、およびビルド方法の説明。

     how_to_add_iris_jdbc_to_eclipse_builtin_mvn_repo.txt
     ビルド時にiris jdbcドライバをローカルのmvn repoに追加する方法

  how_to_debug.txt
  ingest-worker(iris-jdbc-ingest-module)をWindows+Eclipse/vscode環境でデバッグ実行する方法。

  about_ingest_worker.txt  
  INSERT実施の核心部分。

  about_query_worker.txt
  SELECT実施の核心部分。


オリジナルとのマージ  
git remote add upstream https://github.com/intersystems-community/irisdemo-demo-htap.git
git fetch upstream
git merge upstream/master
