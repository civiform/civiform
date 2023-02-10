import setuptools

setuptools.setup(
    name="env_var_docs",
    version="0.1.0",
    packages=["schema"],
    py_modules=["validator", "visitor"],
    install_requires=["jsonschema>=4.17.3"],
    python_requires=">=3.9",
    package_data={"schema": ["schema.json"]},
)
