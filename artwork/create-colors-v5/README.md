# Create纹理与材料颜色 v5

本套最终材质及侧面去点修正已经用户确认采用，随本地正式版1.1.1提交。运行PNG以 `src/main/resources/assets/create_bs/textures/` 为准；本目录提交配色记录、许可及预览，重复导出PNG和交付ZIP留在本地。生成脚本及所需基础样张纳入源码，历史试制目录仅本地保留。

最新微调：按用户游戏截图移除水晶侧面连接贴图中的小铆钉，包括跨CT切片边缘拼成的点。只修改中型和大型侧面PNG各24个原生像素，使用相邻蓝色面板的明暗并按原位置重新计算晶面Alpha；边框、其余晶面像素、正面、顶底面、其他材质和组件均保留。下方初始v5核对记录作为历史保留。

2026-10-10按用户澄清修正v4：原版Minecraft方块仅用于参考颜色和深浅，不将对应方块的图案带入保险库。开发版本仍为1.1.1-dev，已确认的1.1.0安装文件保留。

保留原版Create保险库的框架、铆钉、原有板面纹理及连接图集结构。已移除v4新增的原木年轮、树皮条纹、铁块横纹、金／钻石斜向色带、绿宝石同心图案及黑曜石规则拼块。普通面板使用原Create像素的明暗关系映射到材料调色板，只加入各材质共用的少量轻微色差；不绘制对应方块的专属纹理图案。

材料颜色仍参考原木、铜块、铁块、金块、绿宝石块、钻石块、黑曜石和下界合金块。边框及铆钉保留v4的材料配色。用户已选择的半透明蓝水晶、大块晶面及translucent渲染层保持v4原样；全部56张组件和升级图标也保持v4原样。

共108张保险库PNG和56张物品PNG，运行尺寸16×16／64×64不变，原图集空白RGBA保留，普通保险库Alpha保留，蓝水晶面板Alpha为128／148／168、框架255。使用Python整数像素处理，没有图像生成、随机噪点或抗锯齿。

预览为 `side-colors-v5.png`、`connected-colors-v5.png`、`vaults-create-colors-v5.png`、`blue-crystal-v5.png`，以及同后缀组件／升级图标预览。棋盘格仅用于展示半透明，不在运行贴图中；这些是PNG预览，不是游戏截图。

`create-colors-v5-pngs.zip`包含164张PNG、原版Create许可、导出记录及本说明。`manifest.json`保留配色来源与文件哈希。导出脚本为 `tools/ExportConfirmedTextures.py`，材质处理为 `tools/VaultMaterialDetails.py`；传入`--install`仅替换项目中有变化的资源，逐项备份到 `build/before-create-colors-v5/`。此前v4资源及脚本另存于 `build/before-create-colors-v5.zip`。

本轮源资源与打包核对通过：仅96张普通保险库面板PNG变化，12张蓝水晶、56张物品PNG以及所有框架和点缀像素与v4相同；尺寸、Alpha及图集空白保留。`jar`和`reobfJar`成功，Java编译UP-TO-DATE；最终jar仅这96个PNG条目变化，其余全部条目与v4字节一致。开发客户端读取的`build/resources/main`目录也已与导出逐项核对。结果见`verification-v5.json`。

新版另存为 `build/libs/create-bigger-storage-continued-1.20.1-1.1.1-dev-create-colors-v5.jar`。修改只涉及纹理，不需要调整渲染类或配方；已打开的独立客户端可以手动按F3+T重载查看，无需重启。尚未代替用户操作重载或确认实际视觉效果，没有替换整合包或提交Git。

侧面去点版另存为 `build/libs/create-bigger-storage-continued-1.20.1-1.1.1-dev-crystal-side-clean.jar`。构建与重映射通过，最终jar仅两张水晶侧面PNG条目变化，其余条目与初始v5字节一致；客户端资源目录已同步。预览为 `crystal-side-clean-preview.png`，记录为 `verification-crystal-side-clean.json`。当前实例按F3+T可查看去点结果，无需关闭游戏。
