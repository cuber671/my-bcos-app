package com.fisco.app.util;

import com.fisco.app.security.PasswordUtil;

/**
 * 密码生成工具
 * 用于生成 BCrypt 加密后的密码，方便数据库初始化
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "generate":
            case "gen":
            case "g":
                if (args.length < 2) {
                    System.out.println("❌ 错误: 请提供要加密的密码");
                    printUsage();
                    return;
                }
                String password = args[1];
                String encoded = PasswordUtil.encode(password);
                System.out.println("\n✅ 密码加密成功！");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("明文密码: " + password);
                System.out.println("加密密码: " + encoded);
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("\nSQL 插入语句:");
                System.out.println("INSERT INTO admin (username, password, role, status) VALUES");
                System.out.println("  ('your_username', '" + encoded + "', 'SUPER_ADMIN', 'ACTIVE');");
                System.out.println();
                break;

            case "verify":
            case "v":
                if (args.length < 3) {
                    System.out.println("❌ 错误: 请提供明文密码和加密密码");
                    printUsage();
                    return;
                }
                String rawPassword = args[1];
                String encodedPassword = args[2];
                boolean matches = PasswordUtil.matches(rawPassword, encodedPassword);
                System.out.println("\n" + (matches ? "✅ 密码匹配" : "❌ 密码不匹配"));
                System.out.println("明文密码: " + rawPassword);
                System.out.println("加密密码: " + encodedPassword);
                System.out.println();
                break;

            case "random":
            case "r":
                int length = 12;
                if (args.length >= 2) {
                    try {
                        length = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        System.out.println("❌ 错误: 密码长度必须是数字");
                        return;
                    }
                }
                String randomPassword = PasswordUtil.generateRandomPassword(length);
                String randomEncoded = PasswordUtil.encode(randomPassword);
                System.out.println("\n🎲 随机密码生成成功！");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("明文密码: " + randomPassword);
                System.out.println("加密密码: " + randomEncoded);
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("\n⚠️  请妥善保存明文密码，系统将无法恢复！");
                System.out.println();
                break;

            case "help":
            case "h":
                printUsage();
                break;

            default:
                System.out.println("❌ 未知命令: " + command);
                printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║           密码生成工具 - Password Generator v1.0              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println("\n📖 使用方法:");
        System.out.println("  java PasswordGenerator <command> [arguments]");
        System.out.println("\n📋 命令列表:");
        System.out.println("  generate, gen, g <password>    - 生成 BCrypt 加密密码");
        System.out.println("  verify, v <raw> <encoded>      - 验证明文密码是否匹配加密密码");
        System.out.println("  random, r [length=12]          - 生成随机密码并加密");
        System.out.println("  help, h                       - 显示此帮助信息");
        System.out.println("\n💡 示例:");
        System.out.println("  # 生成加密密码");
        System.out.println("  java PasswordGenerator gen \"MyP@ssw0rd\"");
        System.out.println("\n  # 验证密码");
        System.out.println("  java PasswordGenerator v \"MyP@ssw0rd\" \"$2a$12$...\"");
        System.out.println("\n  # 生成12位随机密码");
        System.out.println("  java PasswordGenerator random");
        System.out.println("\n  # 生成16位随机密码");
        System.out.println("  java PasswordGenerator r 16");
        System.out.println("\n🔒 安全提示:");
        System.out.println("  - 生产环境必须使用强密码（至少12位，包含大小写字母、数字、特殊字符）");
        System.out.println("  - 不要在命令行历史中暴露明文密码");
        System.out.println("  - 首次登录后立即修改默认密码");
        System.out.println();
    }
}
