package com.onectrm.akshare.api.generator;

import net.sourceforge.pinyin4j.PinyinHelper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication
public class AkshareJavaApiGeneratorApplication implements CommandLineRunner {

    @Autowired
    TemplateEngine templateEngine;

    public static void main(String[] args) {
        SpringApplication.run(AkshareJavaApiGeneratorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Options options = new Options();
        options.addOption("docs", true, "akshare documents root folder");
        options.addOption("output", true, "output root folder");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        var files = Files.find(
                        Path.of(cmd.getOptionValue("docs")),
                        Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".md"))
                .collect(Collectors.toList());

        List<AKShareRawAPI> rawAPIS = new LinkedList<>();

        for (var file : files) {
            var lst = parseAKShareRawAPI(file);
            rawAPIS.addAll(lst);
        }

        var apis = rawAPIS.stream().map(api -> generateAPIClass(api)).flatMap(List::stream).collect(Collectors.toList());

        var output = cmd.getOptionValue("output");

        for (AKShareAPIClass api : apis) {
            System.out.println("save class - " + api.getClassName());
            saveAPI(output, api);
        }

        System.out.println("successfully save all classes into " + output);

        return;

    }

    private void saveAPI(String rootFolder, AKShareAPIClass cls) throws IOException {
        var path = Paths.get(rootFolder, cls.getPackageName().replace(".", "/"));
        var folder = Files.createDirectories(path);

        Context context = new Context();
        context.setVariable("api", cls);
        var str = templateEngine.process("AKShareAPIClass", context);

        Files.writeString(Paths.get(folder.toString(), cls.getClassName() + ".java"), str, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private List<AKShareAPIClass> generateAPIClass(AKShareRawAPI rawAPI) {
        var packageName = "com.onectrm.akshare.api.generated." + rawAPI.getCategory();
        var reqClassName = rawAPI.getInterfaceName() + "Request";
        var respClassName = rawAPI.getInterfaceName() + "Response";

        var reqClass = generateAPIClass(packageName, reqClassName, rawAPI.getInput());
        var respClass = generateAPIClass(packageName, respClassName, rawAPI.getOutput());

        reqClass.setRawAPI(rawAPI);
        respClass.setRawAPI(rawAPI);

        return List.of(reqClass, respClass);
    }

    private String toFieldName(String str) {
        StringBuilder sb = new StringBuilder();

        for (var c : str.toCharArray()) {
            var py = PinyinHelper.toHanyuPinyinStringArray(c);

            if (py == null) {
                sb.append(c);
            } else {
                sb.append(py[0]);
            }
        }

        return "p_" + sb.toString().replaceAll("[^a-zA-Z0-9]", "");
    }

    private AKShareAPIClass generateAPIClass(String packageName, String className, List<AKShareRawAPIParameter> rawAPIParameters) {
        AKShareAPIClass cls = new AKShareAPIClass();

        HashMap<AKShareAPIProperty, Integer> count = new HashMap<>();

        if (rawAPIParameters != null) {
            int i = 0;
            for (var param : rawAPIParameters) {
                AKShareAPIProperty property = new AKShareAPIProperty();
                property.setRawAPIParameter(param);
                property.setType(inferType(param));

                var name = StringUtils.trimToNull(param.getName());
                if (StringUtils.isBlank(name)) {
                    name = StringUtils.trimToNull(param.getDescription());
                    if (StringUtils.isBlank(name)) {
                        name = "unknown_param_name_" + i++;
                    }
                }

                property.setName(toFieldName(name));

                if (count.containsKey(property)) {
                    count.put(property, count.get(property) + 1);
                } else {
                    count.put(property, 1);
                }

                cls.getImportedPackages().add(property.getType().getName());
            }
        }

        for (Map.Entry<AKShareAPIProperty, Integer> entry : count.entrySet()) {
            if (entry.getValue() == 1) {
                cls.getProperties().add(entry.getKey());
            }
        }

        cls.setClassName(className);
        cls.setPackageName(packageName);

        return cls;
    }

    private Class inferType(AKShareRawAPIParameter parameter) {

        Class cls = String.class;
        var type = StringUtils.trimToNull(parameter.getType());
        if (StringUtils.isBlank(type)) {
            return cls;
        }
        if ("int64".equalsIgnoreCase(type) || "int".equalsIgnoreCase(type)) {
            cls = Integer.class;
        } else if ("float64".equalsIgnoreCase(type) || "float".equalsIgnoreCase(type)) {
            cls = Double.class;
        } else if ("datetime64".equalsIgnoreCase(type) || "datetime".equalsIgnoreCase(type)) {
            if (isDateTimeType(parameter)) {
                cls = LocalDateTime.class;
            } else {
                cls = LocalDate.class;
            }
        } else {
            if (isDateTimeType(parameter)) {
                cls = LocalDateTime.class;
            } else if (isDateType(parameter)) {
                cls = LocalDate.class;
            }
        }

        return cls;
    }

    boolean isDateType(AKShareRawAPIParameter parameter) {
        var name = StringUtils.toRootLowerCase(parameter.getName());
        var desc = StringUtils.toRootLowerCase(parameter.getDescription());

        return StringUtils.contains(name, "日期") ||
                StringUtils.contains(desc, "日期") ||
                StringUtils.contains(name, "date");
    }

    boolean isDateTimeType(AKShareRawAPIParameter parameter) {
        var name = StringUtils.toRootLowerCase(parameter.getName());
        var desc = StringUtils.toRootLowerCase(parameter.getDescription());

        return StringUtils.contains(name, "时间") ||
                StringUtils.contains(desc, "时间") ||
                StringUtils.contains(name, "time");
    }

    private List<AKShareRawAPI> parseAKShareRawAPI(Path filePath) throws IOException {
        var iterator = Files.lines(filePath).iterator();

        var category = StringUtils.removeEnd(filePath.getFileName().toString(), ".md");
        List<AKShareRawAPI> apis = new ArrayList<>();

        while (iterator.hasNext()) {
            var interfaceName = parse(iterator, "接口:");

            if (StringUtils.isBlank(interfaceName)) {
                break;
            }

            var source = parse(iterator, "目标地址:");
            var desc = parse(iterator, "描述:");
            var retDesc = parse(iterator, "限量:");

            var inputs = parseParameters(iterator, "输入参数");
            var output = parseParameters(iterator, "输出参数");

            AKShareRawAPI api = new AKShareRawAPI();
            api.setCategory(category);
            api.setInterfaceName(interfaceName);
            api.setSource(source);
            api.setDescription(desc);
            api.setReturnDescription(retDesc);

            api.setInput(inputs);
            api.setOutput(output);
            apis.add(api);
        }

        return apis;

    }

    private String parse(Iterator<String> iterator, String token) {
        String val = null;
        while (iterator.hasNext()) {
            var str = iterator.next();
            if (StringUtils.startsWith(str, token)) {
                var strs = StringUtils.split(str, ":", 2);
                if (strs.length == 2) {
                    val = StringUtils.trimToNull(strs[1]);
                }

                break;
            }
        }

        return val;
    }

    private List<AKShareRawAPIParameter> parseParameters(Iterator<String> iterator, String token) {
        List<AKShareRawAPIParameter> parameters = null;
        while (iterator.hasNext()) {
            var str = iterator.next();
            if (StringUtils.startsWith(str, token)) {
                parameters = parseParameterTable(iterator);
                break;
            }
        }

        return parameters;
    }

    private List<AKShareRawAPIParameter> parseParameterTable(Iterator<String> iterator) {
        List<AKShareRawAPIParameter> parameters = new ArrayList<>();

        boolean flag = true;

        while (flag && iterator.hasNext()) {
            var line = iterator.next();
            line = StringUtils.replace(line, " ", "");

            if (StringUtils.startsWith(line, "|--")) {
                while (iterator.hasNext()) {
                    var tableLine = iterator.next();
                    var parameter = parseParameterTableRow(tableLine);
                    if (parameter == null) {
                        flag = false;
                        break;
                    }

                    parameters.add(parameter);
                }
            }
        }

        return parameters.size() == 0 ? null : parameters;
    }

    private AKShareRawAPIParameter parseParameterTableRow(String str) {

        if (StringUtils.isBlank(str)) {
            return null;
        }

        var strs = StringUtils.split(str, "|");

        if (Arrays.stream(strs).allMatch(s -> StringUtils.isBlank(s))) {
            return null;
        }

        Function<String, String> extract = (val) -> {
            var s = StringUtils.trim(val);
            if (StringUtils.isBlank(s) || s.equalsIgnoreCase("-")) {
                s = null;
            }

            return s;
        };

        AKShareRawAPIParameter parameter = null;

        if (strs.length >= 3) {
            var name = extract.apply(strs[0]);
            var type = extract.apply(strs[1]);
            var desc = strs.length == 3 ? extract.apply(strs[2]) : extract.apply(strs[3]);
            var required = strs.length == 3 ? null : extract.apply(strs[2]);

            if (name != null || type != null || desc != null || required != null) {
                parameter = new AKShareRawAPIParameter();
                parameter.setName(name);
                parameter.setType(type);
                parameter.setRequired(required);
                parameter.setDescription(desc);
            }
        }

        return parameter;
    }


}
