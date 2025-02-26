def get_prompt(text):
    return f"""
        You are an json expert. Format the following json to be easier to read:

        {text}

        put the attributes of each field in one line

        Output only JSON, no explanations.
        """
