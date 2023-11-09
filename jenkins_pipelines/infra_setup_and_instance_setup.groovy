pipeline {
    agent any
    tools {
        terraform 'tf1.6'
    }

    stages {
        stage('Clone Git repo') {
            steps {
                git(
                    branch: 'main', 
                    url: 'https://github.com/glass91/jenkinsNorm.git', 
                    credentialsId: 'acces_to_git'
                )
            }
        }
        
        stage('Install Ansible') {
            steps {
        sh '''
        sudo apt-add-repository ppa:ansible/ansible -y
        sudo apt-get update
        sudo apt-get install ansible -y
        '''
            }
        }

        stage('Terraform Plan') {
            steps {
                sh '''
                cd /var/lib/jenkins/workspace/red_page_html/terraform_ansible_generic_instace_setup_template
                echo "yes" | terraform init
                terraform plan -out=terraform.tfplan
                '''
            }
        }

        stage('Plan verification and user input') {
            steps {
                input(
                    message: 'proceed or abort?', 
                    ok: 'ok'
                )
            }
        }

        stage('Terraform Apply') {
            steps {
                sh '''
                cd /var/lib/jenkins/workspace/apchwebsite2/terraform_ansible_generic_instace_setup_template
                terraform apply terraform.tfplan
                '''
            }
        }

        stage('Get Terraform Outputs') {
            steps {
                sh '''
                cd /var/lib/jenkins/workspace/apchwebsite2/terraform_ansible_generic_instace_setup_template
                terraform output web-address-nodejs > ./ansible/instance_ip.txt
                '''
            }
        }

        stage('Run Ansible') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-ansible', keyFileVariable: 'SSH_KEY')]) {
                    sh '''
                    sleep 180
                    cd /var/lib/jenkins/workspace/apchwebsite2/terraform_ansible_generic_instace_setup_template/ansible
                    ansible-playbook -i instance_ip.txt playbook_apache.yaml -u ubuntu --private-key=$SSH_KEY -e 'ansible_ssh_common_args="-o StrictHostKeyChecking=no"'
                    '''
                }
            }
        }
    }
}
