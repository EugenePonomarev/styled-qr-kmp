version_line = File.readlines(File.join(__dir__, "gradle.properties"))
  .find { |line| line.start_with?("VERSION_NAME=") }


raise "VERSION_NAME is missing from gradle.properties" if version_line.nil?

version_name = version_line.split("=", 2).last.strip
raise "VERSION_NAME must not be blank" if version_name.empty?

Pod::Spec.new do |spec|
  spec.name                  = "StyledQrKmp"
  spec.version               = version_name
  spec.summary               = "Dependency-free Kotlin Multiplatform QR-code generator with styled rendering."
  spec.description           = <<-DESC
Styled QR KMP creates QR codes without ZXing or another QR-generation library.
The prebuilt framework includes the shared encoder and UIKit adapter.
  DESC
  spec.homepage              = "https://github.com/EugenePonomarev/styled-qr-kmp"
  spec.license               = { :type => "MIT", :file => "LICENSE" }
  spec.author                = { "Evgenii Ponomarev" => "eugeneponomarevdev@gmail.com" }
  spec.platform              = :ios, "13.0"
  spec.swift_version         = "5.0"
  spec.static_framework      = true
  spec.source                = {
    :http => "https://github.com/EugenePonomarev/styled-qr-kmp/releases/download/#{spec.version}/StyledQrKmp.xcframework.zip"
  }
  spec.vendored_frameworks   = "StyledQrKmp.xcframework"
end
