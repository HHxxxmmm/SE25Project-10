# 前端单元测试覆盖率工具使用说明

本项目前端单元测试采用 [Jest](https://jestjs.io/) 作为测试框架，并集成了覆盖率统计功能，帮助开发者了解代码的测试覆盖情况。

## 覆盖率工具简介

Jest 内置了代码覆盖率统计功能，能够自动分析哪些代码被测试覆盖，哪些未被覆盖。覆盖率报告包括以下几项：

- **Statements**（语句覆盖率）：被执行的代码语句比例
- **Branches**（分支覆盖率）：条件分支（如 if/else）被测试的比例
- **Functions**（函数覆盖率）：被调用的函数比例
- **Lines**（行覆盖率）：被执行的代码行比例

## 如何生成覆盖率报告

在项目根目录下，运行以下命令即可生成覆盖率报告：

```bash
npm test -- --coverage
# 或者
yarn test --coverage
```

执行后，Jest 会自动统计所有测试的覆盖率，并在控制台输出简要报告，同时生成详细的 HTML 报告。

## 查看覆盖率报告

覆盖率报告默认输出在 `coverage/` 目录下。打开 `coverage/lcov-report/index.html` 可以在浏览器中查看详细的可视化报告，包括每个文件的覆盖率详情。

## 配置覆盖率阈值（可选）

可以在 `package.json` 或 Jest 配置文件中设置覆盖率阈值，强制要求测试覆盖率达到一定标准。例如：

```json
"jest": {
  "collectCoverage": true,
  "coverageThreshold": {
    "global": {
      "branches": 80,
      "functions": 80,
      "lines": 80,
      "statements": 80
    }
  }
}
```

## 常见问题

- **覆盖率低怎么办？**  
  增加更多针对未覆盖代码的测试用例，尤其是分支和异常处理逻辑。

- **某些文件不需要统计覆盖率？**  
  可通过 `collectCoverageFrom` 配置排除特定文件或目录。

## 参考链接

- [Jest 官方文档 - Coverage](https://jestjs.io/docs/zh-Hans/coverage)
- [Jest 配置选项](https://jestjs.io/docs/zh-Hans/configuration)

---
如需进一步帮助，请查阅项目内的测试代码或联系开发团队。
